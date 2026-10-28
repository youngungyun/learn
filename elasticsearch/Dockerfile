# 한국어 형태소 분석기(nori) 플러그인이 들어간 Elasticsearch.
# docker compose 와 Testcontainers 테스트가 같은 파일을 쓰므로 "내 PC 에서는 되는데 테스트에서는 다른" 일이 없다.
FROM docker.elastic.co/elasticsearch/elasticsearch:9.4.8
RUN bin/elasticsearch-plugin install --batch analysis-nori
