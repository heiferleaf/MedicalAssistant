# MedicalAssistant 基于Agent的医药助手 -- 后端部分

---

## 架构简要说明

- 业务逻辑架构：后端部分采用 `SprintBoot` 架构，使用经典的 `MVC` 分层设计
- 数据库：通过添加 `MyBatis` 依赖，连接 `MySQL` 数据库

## MVC 具体说明

> 按照业务处理流程说明

- `dto` :处理前端通过 `HTTP` 请求，传给后端的`get`URL参数或者`post`请求体参数
- `controller` :处理器层，通过`Spring Validation`的参数校验请求，会使用处理器持有的`service`，对Tomcat服务器收到的HTTP请求进行处理
- `service` :逻辑业务处理，对控制器收到的数据，借助`repository`，进行业务逻辑处理
- `repository` :数据仓库层，内部持有`mapper`，主要实现`service`业务需求和实际`mapper`得到数据的格式转换
- `mapper` :映射层，通过配置文件，连接数据库，对数据库进行操作
- `entity` :实体包，存放数据库中，关系模式对应的Java类,也是`mapper`使用和返回的数据类型
- `exception` :异常包，存放自定义的异常，以及全局的异常处理器。在`controller`收到的异常，会直接进入处理器的对应方法
- `commom` :公共包，存放所有业务通过的返回数据包装类，包括状态码的枚举类和返回数据的封装类
- `util` :工具包，存放`service`层处理业务时，需要使用的工具
- `config` :配置包，存放执行定时任务调度线程池的配置类
- `schedule` :任务调度包，实现每日定时生成任务，并配置每一个任务的超时漏服处理
- `handler` :类型处理包，实现了`entity`中，`LocalTime / List<LocalTime>` \<--> `JdbcType.VARCHAR` 的转换。具体在`MyBatis`的`xml`文件中配置使用