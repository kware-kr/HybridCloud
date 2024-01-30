스케줄링 API를 위한 환경 구성 스크립트들입니다.

GPU(nvidia dcgm-exporter)활용시 유의사항
- kubenetest v22에서는 cuda 12.2.2를 설치하고, 관련 드라이버를 설치( 
  Driver Version: 535.104.05 CUDA Version: 12.2)하면 동작이 잘 된다.

#### 구성

1. `init`: 쿠버네티스 설치를 위한 스크립트들입니다.
2. `api`: API와 Postgresql 데이터베이스 설치 스크립트들입니다.
3. `prom`: 프로메테우스 설치를 위한 스크립트들입니다.
