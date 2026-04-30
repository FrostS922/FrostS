# Docker中间件管理脚本

## 启动中间件
docker-compose -f docker-compose.middleware.yml up -d

## 查看状态
docker-compose -f docker-compose.middleware.yml ps

## 查看日志
docker-compose -f docker-compose.middleware.yml logs -f

## 停止中间件
docker-compose -f docker-compose.middleware.yml down

## 重启中间件
docker-compose -f docker-compose.middleware.yml restart

## 连接PostgreSQL
docker exec -it test-platform-db psql -U postgres -d test_platform

## 连接Redis
docker exec -it test-platform-redis redis-cli -p 6379
