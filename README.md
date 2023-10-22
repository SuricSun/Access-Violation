# PINK2
## 使用方法
```
服务端 java -jar Server.jar
客户端 java -jar Client.jar
访问客户端的代理端口即可
```
## 路由配置
```json
{
	"proxy_data_multiplexing_listen_port": 8888,
	"router": [
		{
                        "#": "从任意地址",
			"from_regex": ".+",
                        "#": "映射到注册名为CLIENT_PINK的主机",
			"to": "CLIENT_PINK"
		}
	],
	"thread_pool": {
		"core_pool_size": 32,
		"maximum_pool_size": 64,
		"queue_size": 32,
		"keep_alive_time_sec": 60
	}
}
```
