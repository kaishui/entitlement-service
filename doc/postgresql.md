docker run --name postgres \
-e POSTGRES_USER=myuser \
-e POSTGRES_PASSWORD=zijing \
-e POSTGRES_DB=postgres \
-p 5432:5432 \
-v pgdata:/Users/kaishui/Downloads/xueyue/data \
-d postgres:16
