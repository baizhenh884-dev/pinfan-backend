package com.pinfan.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.pinfan.common.exception.BusinessException;
import com.pinfan.entity.SeckillVoucher;
import com.pinfan.entity.VoucherOrder;
import com.pinfan.mapper.VoucherOrderMapper;
import com.pinfan.service.SeckillVoucherService;
import com.pinfan.service.VoucherOrderService;
import com.pinfan.utils.RedisIdWorker;
import com.pinfan.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder>
                                     implements VoucherOrderService {
    @Autowired
    private SeckillVoucherService seckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> SECKILL_STREAM_SCRIPT;
    static {
        SECKILL_STREAM_SCRIPT = new DefaultRedisScript<>();
        SECKILL_STREAM_SCRIPT.setLocation(new ClassPathResource("seckill-stream.lua"));
        SECKILL_STREAM_SCRIPT.setResultType(Long.class);
    }

    // 单线程消费者
    private static final ExecutorService STREAM_CONSUMER_EXECUTOR =
            Executors.newSingleThreadExecutor();

    private static final String STREAM_KEY = "stream.orders";
    private static final String GROUP_NAME = "g1";
    private static final String CONSUMER_NAME = "c1";

    @Override
    public Long seckillVoucherV1(Long voucherId) {
        // 查秒杀券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) {
            throw new BusinessException(404, "秒杀券不存在");
        }

        // 时间校验
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) {
            throw new BusinessException(400, "秒杀尚未开始");
        }
        if (now.isAfter(voucher.getEndTime())) {
            throw new BusinessException(400, "秒杀已经结束");
        }

        // 库存校验
        if (voucher.getStock() < 1) {
            throw new BusinessException(400, "库存不足");
        }

        // 一人一单 + 扣库存 + 创建订单
        Long userId = UserHolder.getUser().getId();

        // 关键：锁 userId 字符串（.intern() 保证同一 userId 是同一把锁对象）
        synchronized (userId.toString().intern()) {
            // 一人一单校验
            long count = query()
                    .eq("user_id", userId)
                    .eq("voucher_id", voucherId)
                    .count();
            if (count > 0) {
                throw new BusinessException(400, "您已经抢过这张券了");
            }

            // 扣库存
            boolean success = seckillVoucherService.update()
                    .setSql("stock = stock - 1")
                    .eq("voucher_id", voucherId)
                    .update();
            if (!success) {
                throw new BusinessException(500, "扣减库存失败");
            }

            // 创建订单
            VoucherOrder order = new VoucherOrder();
            order.setId(redisIdWorker.nextId("order"));
            order.setUserId(userId);
            order.setVoucherId(voucherId);
            save(order);

            log.info("V1 抢券成功: userId={}, voucherId={}, orderId={}", userId, voucherId, order.getId());
            return order.getId();
        }
    }

    @Override
    public Long seckillVoucherV2(Long voucherId) {
        // 校验（同V1）
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) throw new BusinessException(404, "秒杀券不存在");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) throw new BusinessException(400, "秒杀尚未开始");
        if (now.isAfter(voucher.getEndTime())) throw new BusinessException(400, "秒杀已经结束");
        if (voucher.getStock() < 1) throw new BusinessException(400, "库存不足");

        // 4. synchronized 锁 userId
        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            // 关键：通过 AOP 代理调本类方法，才能触发 @Transactional
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
        // 锁在整个return之后才释放，事务已经在return前提交
        // 所以其他线程读到的是已提交的数据
    }

    @Transactional
    @Override
    public Long createVoucherOrder(Long voucherId) {
        Long userId = UserHolder.getUser().getId();

        // 一人一单校验
        long count = query()
                .eq("user_id", userId)
                .eq("voucher_id", voucherId)
                .count();
        if (count > 0) {
            throw new BusinessException(400, "您已经抢过这张券了");
        }

        // 扣库存加 CAS 条件：stock > 0 才更新
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)         // ← V2 关键
                .update();
        if (!success) {
            throw new BusinessException(400, "库存不足");
        }

        // 创建订单
        VoucherOrder order = new VoucherOrder();
        order.setId(redisIdWorker.nextId("order"));
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        save(order);

        log.info("V2 抢券成功: userId={}, voucherId={}, orderId={}", userId, voucherId, order.getId());
        return order.getId();
    }

    @Override
    public Long seckillVoucherV3(Long voucherId) {
        // 基础校验
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) throw new BusinessException(404, "秒杀券不存在");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) throw new BusinessException(400, "秒杀尚未开始");
        if (now.isAfter(voucher.getEndTime())) throw new BusinessException(400, "秒杀已经结束");

        Long userId = UserHolder.getUser().getId();

        // 执行 Lua 脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                List.of("seckill:stock:" + voucherId, "seckill:order:" + voucherId),
                userId.toString()
        );

        if (result == null || result.intValue() != 0) {
            int code = result == null ? 500 : result.intValue();
            String msg = code == 1 ? "库存不足" : (code == 2 ? "您已经抢过这张券了" : "抢购失败");
            throw new BusinessException(400, msg);
        }

        // Redis通过，DB落库（通过 AOP 代理调事务方法）
        VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
        return proxy.createOrderFromRedis(voucherId, userId);
    }

    @Transactional
    @Override
    public Long createOrderFromRedis(Long voucherId, Long userId) {
        // DB 扣库存（CAS 兜底，理论上 Redis 已经保证不超卖）
        boolean success = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock", 0)
                .update();
        if (!success) {
            throw new BusinessException(500, "库存扣减失败（DB 与 Redis 数据不一致，需人工介入）");
        }

        // 创建订单
        VoucherOrder order = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        order.setId(orderId);
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        save(order);

        log.info("V3 抢券成功: userId={}, voucherId={}, orderId={}", userId, voucherId, orderId);
        return orderId;
    }

    @Override
    public Long seckillVoucherV4(Long voucherId) {
        // 基础校验
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) throw new BusinessException(404, "秒杀券不存在");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) throw new BusinessException(400, "秒杀尚未开始");
        if (now.isAfter(voucher.getEndTime())) throw new BusinessException(400, "秒杀已经结束");
        if (voucher.getStock() < 1) throw new BusinessException(400, "库存不足");

        Long userId = UserHolder.getUser().getId();

        // 拿 Redisson 分布式锁（每个用户一把）
        RLock lock = redissonClient.getLock("lock:seckill:user:" + userId);
        boolean isLock = false;
        try {
            // waitTime=0 抢不到立刻失败，leaseTime=10 秒自动释放防死锁
            isLock = lock.tryLock(0, 10, TimeUnit.SECONDS);
            if (!isLock) {
                throw new BusinessException(400, "请勿重复下单");
            }

            // 拿到锁 → 通过 AOP 代理调事务方法
            VoucherOrderService proxy = (VoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);   // 复用 V2 的事务方法

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(500, "抢购被中断");
        } finally {
            // finally 释放锁
            if (isLock) {
                lock.unlock();
            }
        }
    }

    @PostConstruct
    public void initConsumer() {
        // 1. 确保 Stream 存在 + 创建 Consumer Group
        try {
            stringRedisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
            log.info("Stream consumer group 创建成功: {}", GROUP_NAME);
        } catch (Exception e) {
            // 已存在时会抛异常，忽略即可
            log.info("Stream consumer group 已存在，跳过创建");
        }

        // 2. 启动消费者线程
        STREAM_CONSUMER_EXECUTOR.submit(this::consumeStream);
    }

    private void consumeStream() {
        log.info("V5 Stream 消费者启动");
        while (true) {
            try {
                // XREADGROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS stream.orders >
                List<MapRecord<String, Object, Object>> records = stringRedisTemplate
                        .opsForStream()
                        .read(
                                Consumer.from(GROUP_NAME, CONSUMER_NAME),
                                StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                        );

                if (records == null || records.isEmpty()) {
                    continue;   // 没消息，继续等
                }

                MapRecord<String, Object, Object> record = records.get(0);
                handleOrder(record);

                // ACK 确认
                stringRedisTemplate.opsForStream()
                        .acknowledge(STREAM_KEY, GROUP_NAME, record.getId());

            } catch (Exception e) {
                log.error("消费 Stream 消息失败", e);
                // 生产环境要处理 pending list 重试，这里简化
            }
        }
    }

    private void handleOrder(MapRecord<String, Object, Object> record) {
        Map<Object, Object> values = record.getValue();
        VoucherOrder order = new VoucherOrder();
        order.setId(Long.parseLong((String) values.get("id")));
        order.setUserId(Long.parseLong((String) values.get("userId")));
        order.setVoucherId(Long.parseLong((String) values.get("voucherId")));

        // DB 扣库存
        seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", order.getVoucherId())
                .gt("stock", 0)
                .update();

        // DB 插订单
        save(order);
        log.info("V5 消费者落库成功: orderId={}, userId={}", order.getId(), order.getUserId());
    }

    @Override
    public Long seckillVoucherV5(Long voucherId) {
        // 1. 基础校验
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        if (voucher == null) throw new BusinessException(404, "秒杀券不存在");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) throw new BusinessException(400, "秒杀尚未开始");
        if (now.isAfter(voucher.getEndTime())) throw new BusinessException(400, "秒杀已经结束");

        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");   // 预生成 orderId

        // 2. 执行 Lua 脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_STREAM_SCRIPT,
                List.of(
                        "seckill:stock:" + voucherId,
                        "seckill:order:" + voucherId,
                        STREAM_KEY
                ),
                voucherId.toString(),
                userId.toString(),
                String.valueOf(orderId)
        );

        if (result == null || result != 0) {
            long code = result == null ? -1 : result;
            String msg = code == 1 ? "库存不足" : (code == 2 ? "您已经抢过这张券了" : "抢购失败");
            throw new BusinessException(400, msg);
        }

        // 3. 立即返回，DB 落库由后台消费者异步处理
        log.info("V5 消息已投递: userId={}, voucherId={}, orderId={}", userId, voucherId, orderId);
        return orderId;
    }
}
