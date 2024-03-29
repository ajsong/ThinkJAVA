//Developed by @mario 1.2.20220301
package com.framework.tool;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;
import org.springframework.util.CollectionUtils;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class Redis {

	private static RedisTemplate<String, Object> redisTemplate;
	private RedisTemplate<String, Object> redis;

	public Redis() {
		if (redisTemplate == null) {
			boolean enabled = Common.getYml("spring.redis.enabled", false);
			if (!enabled) return;
			int database = Integer.parseInt(Common.getYml("spring.redis.database", ""));
			String host = Common.getYml("spring.redis.host", "");
			int port = Integer.parseInt(Common.getYml("spring.redis.port", ""));
			String password = Common.getYml("spring.redis.password", "");
			redisTemplate = init(database, host, port, password);
		}
		redis = redisTemplate;
	}

	public Redis(int database, String host, int port) {
		redis = init(database, host, port, "");
	}

	public Redis(int database, String host, int port, String password) {
		redis = init(database, host, port, password);
	}

	public RedisTemplate<String, Object> init(int database, String host, int port, String password) {
		RedisStandaloneConfiguration rsc = new RedisStandaloneConfiguration();
		rsc.setDatabase(database);
		rsc.setHostName(host);
		rsc.setPort(port);
		if (password != null && password.length() > 0) rsc.setPassword(password);
		JedisConnectionFactory fac = new JedisConnectionFactory(rsc);
		fac.afterPropertiesSet();
		return getredis(fac);
	}

	@SuppressWarnings("all")
	public RedisTemplate<String, Object> getredis(JedisConnectionFactory factory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
		ObjectMapper om = new ObjectMapper();
		om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
		om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
		jackson2JsonRedisSerializer.setObjectMapper(om);
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		//key采用String的序列化方式
		template.setKeySerializer(stringRedisSerializer);
		//hash的key也采用String的序列化方式
		template.setHashKeySerializer(stringRedisSerializer);
		//value序列化方式采用jackson
		template.setValueSerializer(jackson2JsonRedisSerializer);
		//hash的value序列化方式采用jackson
		template.setHashValueSerializer(jackson2JsonRedisSerializer);
		template.afterPropertiesSet();
		return template;
	}

	// =============================common通用的============================

	/**
	 * 判断心跳
	 */
	public boolean ping() {
		if (redis == null) return false;
		try {
			return Objects.equals(redis.execute(RedisConnectionCommands::ping), "PONG");
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 指定缓存失效时间
	 * @param key  键
	 * @param time 时间(秒)
	 */
	public boolean expire(String key, long time) {
		try {
			if (time > 0) {
				redis.expire(key, time, TimeUnit.SECONDS);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 根据key 获取过期时间
	 * @param key 键 不能为null
	 * @return 时间(秒) 返回0代表为永久有效
	 */
	public Long getExpire(String key) {
		return redis.getExpire(key, TimeUnit.SECONDS);
	}

	/**
	 * 判断key是否存在
	 * @param key 键
	 * @return true 存在 false不存在
	 */
	public boolean hasKey(String key) {
		try {
			return Boolean.TRUE.equals(redis.hasKey(key));
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 获取所有key
	 */
	public Set<String> getKeys() {
		return redis.keys("*");
	}

	/**
	 * 获取所有值
	 */
	public List<Object> getValues() {
		Set<String> keys = redis.keys("*");
		if (keys == null) return null;
		return redis.opsForValue().multiGet(keys);
	}

	/**
	 * 根据前缀获取所有key
	 * 例如：pre_、/css/
	 */
	public Set<String> getListKey(String... prefix) {
		List<String> keys = new ArrayList<>();
		if (prefix == null || prefix.length == 0) return null;
		for (String p : prefix) {
			Set<String> key = redis.keys(p.concat("*"));
			if (key == null) continue;
			keys.addAll(key);
		}
		if (keys.size() == 0) return null;
		return new HashSet<>(keys);
	}

	/**
	 * 根据前缀获取所有值
	 */
	public List<Object> getListValue(String... prefix) {
		Set<String> keys = getListKey(prefix);
		if (keys == null) return null;
		return redis.opsForValue().multiGet(keys);
	}

	/**
	 * 删除缓存
	 * @param key 可以传一个值 或多个
	 */
	@SuppressWarnings("unchecked")
	public void del(String... key) {
		if (key != null && key.length > 0) {
			if (key.length == 1) {
				redis.delete(key[0]);
			} else {
				redis.delete((Collection<String>) CollectionUtils.arrayToList(key));
			}
		}
	}

	// ============================String=============================
	/**
	 * 普通缓存获取
	 * @param key 键
	 * @return 值
	 */
	public Object get(String key) {
		return key == null ? null : redis.opsForValue().get(key);
	}

	/**
	 * 普通缓存放入
	 * @param key   键
	 * @param value 值
	 * @return true成功 false失败
	 */
	public boolean set(String key, Object value) {
		try {
			redis.opsForValue().set(key, value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 普通缓存放入并设置时间
	 * @param key   键
	 * @param value 值
	 * @param time  时间(秒) time要大于0 如果time小于等于0 将设置无限期
	 * @return true成功 false 失败
	 */
	public boolean set(String key, Object value, long time) {
		try {
			if (time > 0) {
				redis.opsForValue().set(key, value, time, TimeUnit.SECONDS);
			} else {
				set(key, value);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 递增
	 * @param key   键
	 * @param delta 要增加几(大于0)
	 */
	public Long incr(String key, long delta) {
		if (delta < 0) {
			throw new RuntimeException("递增因子必须大于0");
		}
		return redis.opsForValue().increment(key, delta);
	}

	/**
	 * 递减
	 * @param key   键
	 * @param delta 要减少几(小于0)
	 */
	public Long decr(String key, long delta) {
		if (delta < 0) {
			throw new RuntimeException("递减因子必须大于0");
		}
		return redis.opsForValue().increment(key, -delta);
	}

	// ================================Map 就是hash=================================
	/**
	 * HashGet
	 * @param key  键 不能为null
	 * @param item 项 不能为null
	 * @return 值
	 */
	public Object hget(String key, String item) {
		return redis.opsForHash().get(key, item);
	}

	/**
	 * 获取hashKey对应的所有键值
	 * @param key 键
	 * @return 对应的多个键值
	 */
	public Map<Object, Object> hmget(String key) {
		return redis.opsForHash().entries(key);
	}

	/**
	 * HashSet
	 * @param key 键
	 * @param map 对应多个键值
	 * @return true 成功 false 失败
	 */
	public boolean hmset(String key, Map<String, Object> map) {
		try {
			redis.opsForHash().putAll(key, map);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * HashSet 并设置时间
	 * @param key  键
	 * @param map  对应多个键值
	 * @param time 时间(秒)
	 * @return true成功 false失败
	 */
	public boolean hmset(String key, Map<String, Object> map, long time) {
		try {
			redis.opsForHash().putAll(key, map);
			if (time > 0) expire(key, time);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 向一张hash表中放入数据,如果不存在将创建
	 * @param key   键
	 * @param item  项
	 * @param value 值
	 * @return true 成功 false失败
	 */
	public boolean hset(String key, String item, Object value) {
		try {
			redis.opsForHash().put(key, item, value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 向一张hash表中放入数据,如果不存在将创建
	 * @param key   键
	 * @param item  项
	 * @param value 值
	 * @param time  时间(秒) 注意:如果已存在的hash表有时间,这里将会替换原有的时间
	 * @return true 成功 false失败
	 */
	public boolean hset(String key, String item, Object value, long time) {
		try {
			redis.opsForHash().put(key, item, value);
			if (time > 0) expire(key, time);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 删除hash表中的值
	 * @param key  键 不能为null
	 * @param item 项 可以使多个 不能为null
	 */
	public void hdel(String key, Object... item) {
		redis.opsForHash().delete(key, item);
	}

	/**
	 * 判断hash表中是否有该项的值
	 * @param key  键 不能为null
	 * @param item 项 不能为null
	 * @return true 存在 false不存在
	 */
	public boolean hHasKey(String key, String item) {
		return redis.opsForHash().hasKey(key, item);
	}

	/**
	 * hash递增 如果不存在,就会创建一个 并把新增后的值返回
	 * @param key  键
	 * @param item 项
	 * @param by   要增加几(大于0)
	 */
	public double hincr(String key, String item, double by) {
		return redis.opsForHash().increment(key, item, by);
	}

	/**
	 * hash递减
	 * @param key  键
	 * @param item 项
	 * @param by   要减少记(小于0)
	 */
	public double hdecr(String key, String item, double by) {
		return redis.opsForHash().increment(key, item, -by);
	}

	// ============================set=============================
	/**
	 * 根据key获取Set中的所有值
	 * @param key 键
	 */
	public Set<Object> sGet(String key) {
		try {
			return redis.opsForSet().members(key);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 根据value从一个set中查询,是否存在
	 * @param key   键
	 * @param value 值
	 * @return true 存在 false不存在
	 */
	public Boolean sHasKey(String key, Object value) {
		try {
			return redis.opsForSet().isMember(key, value);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 将数据放入set缓存
	 * @param key    键
	 * @param values 值 可以是多个
	 * @return 成功个数
	 */
	public Long sSet(String key, Object... values) {
		try {
			return redis.opsForSet().add(key, values);
		} catch (Exception e) {
			return Long.parseLong("0");
		}
	}

	/**
	 * 将set数据放入缓存
	 * @param key    键
	 * @param time   时间(秒)
	 * @param values 值 可以是多个
	 * @return 成功个数
	 */
	public Long sSetAndTime(String key, long time, Object... values) {
		try {
			Long count = redis.opsForSet().add(key, values);
			if (time > 0) expire(key, time);
			return count;
		} catch (Exception e) {
			return Long.parseLong("0");
		}
	}

	/**
	 * 获取set缓存的长度
	 * @param key 键
	 */
	public Long sGetSetSize(String key) {
		try {
			return redis.opsForSet().size(key);
		} catch (Exception e) {
			return Long.parseLong("0");
		}
	}

	/**
	 * 移除值为value的
	 * @param key    键
	 * @param values 值 可以是多个
	 * @return 移除的个数
	 */
	public Long setRemove(String key, Object... values) {
		try {
			return redis.opsForSet().remove(key, values);
		} catch (Exception e) {
			return Long.parseLong("0");
		}
	}

	// ===============================list=================================
	/**
	 * 获取list缓存的内容
	 * @param key   键
	 * @param start 开始
	 * @param end   结束 0 到 -1代表所有值
	 */
	public List<Object> lGet(String key, long start, long end) {
		try {
			return redis.opsForList().range(key, start, end);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 获取list缓存的长度
	 * @param key 键
	 */
	public Long lGetListSize(String key) {
		try {
			return redis.opsForList().size(key);
		} catch (Exception e) {
			return Long.parseLong("0");
		}
	}

	/**
	 * 通过索引 获取list中的值
	 * @param key   键
	 * @param index 索引 index>=0时， 0 表头，1 第二个元素，依次类推；index<0时，-1，表尾，-2倒数第二个元素，依次类推
	 */
	public Object lGetIndex(String key, long index) {
		try {
			return redis.opsForList().index(key, index);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 将list放入缓存
	 * @param key   键
	 * @param value 值
	 */
	public boolean lSet(String key, Object value) {
		try {
			redis.opsForList().rightPush(key, value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 将list放入缓存
	 * @param key   键
	 * @param value 值
	 * @param time  时间(秒)
	 */
	public boolean lSet(String key, Object value, long time) {
		try {
			redis.opsForList().rightPush(key, value);
			if (time > 0) expire(key, time);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 将list放入缓存
	 * @param key   键
	 * @param value 值
	 */
	public boolean lSet(String key, List<Object> value) {
		try {
			redis.opsForList().rightPushAll(key, value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 将list放入缓存
	 * @param key   键
	 * @param value 值
	 * @param time  时间(秒)
	 */
	public boolean lSet(String key, List<Object> value, long time) {
		try {
			redis.opsForList().rightPushAll(key, value);
			if (time > 0) expire(key, time);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 根据索引修改list中的某条数据
	 * @param key   键
	 * @param index 索引
	 * @param value 值
	 */
	public boolean lUpdateIndex(String key, long index, Object value) {
		try {
			redis.opsForList().set(key, index, value);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 移除N个值为value
	 * @param key   键
	 * @param count 移除多少个
	 * @param value 值
	 * @return 移除的个数
	 */
	public Long lRemove(String key, long count, Object value) {
		try {
			return redis.opsForList().remove(key, count, value);
		} catch (Exception e) {
			return Long.parseLong("0");
		}
	}
}

