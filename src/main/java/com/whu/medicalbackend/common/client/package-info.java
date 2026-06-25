/**
 * 服务间通信客户端包
 *
 * 本包包含用于微服务间 HTTP 调用的 Feign Client 接口和配置。
 *
 * 设计原则：
 * 1. 每个领域服务提供内部 API（/internal/*）供其他服务调用
 * 2. 使用 RestTemplate 进行服务间同步调用（暂不引入 Spring Cloud）
 * 3. 服务地址通过配置文件管理（application-{profile}.yml）
 * 4. 所有跨服务调用都应该通过本包的 Client 接口
 *
 * @author Medical Assistant Team
 * @since 1.0
 */
package com.whu.medicalbackend.common.client;
