<script setup>
const rows = [
  ['/demo/axios', 'axios', '인스턴스 + 인터셉터, 타임아웃, 요청 취소, 병렬 호출', '대부분의 프로젝트의 기본 선택', '현재 앱 전체'],
  ['/demo/fetch', 'fetch (브라우저 내장)', '의존성 없음, res.ok 직접 확인, AbortController, 디바운스 자동완성', '라이브러리 없이 가볍게', '-'],
  ['/demo/composable', '컴포저블 useApi', 'loading / error / data 를 재사용, 경쟁 상태(race) 방지', '화면마다 반복되는 상태 코드 제거', '-'],
  ['/demo/pinia', 'Pinia 스토어', '형제 컴포넌트가 같은 서버 데이터를 공유', '여러 화면/컴포넌트가 같은 데이터를 쓸 때', '-'],
  ['/demo/query', 'TanStack Query', '캐시, staleTime, 자동 재조회, 재시도, mutation 후 무효화', '서버 데이터 조회가 많은 화면', '-'],
  ['/demo/jwt', 'JWT Bearer 토큰', '쿠키 없이 Authorization 헤더로 인증', '모바일/타 도메인/외부 클라이언트', '백엔드 /api/auth/token'],
  ['/demo/realtime', 'SSE vs 폴링', '서버가 먼저 알림을 푸시 (이체 수신 알림)', '실시간 알림, 진행 상태', '백엔드 /api/notifications/stream'],
  ['/demo/download', '파일 다운로드 (Blob)', '인증이 필요한 CSV 파일 내려받기, 진행률', '엑셀/CSV/PDF 내보내기', '백엔드 /api/transactions/export']
]
</script>

<template>
  <div class="card">
    <h2>백엔드 호출 방식 데모</h2>
    <p class="desc">같은 Spring Boot API 를 여러 방식으로 호출해 보는 화면 모음입니다. 각 화면에서 <b>핵심 코드 보기</b>를 펼치면 실제로 쓰인 코드를 볼 수 있습니다.
      (로그인한 상태에서 진행하세요 — 데모 계정 hong / Hong1234!)</p>
    <table>
      <thead><tr><th>화면</th><th>방식</th><th>보여주는 것</th><th>이럴 때</th><th>백엔드 추가</th></tr></thead>
      <tbody>
        <tr v-for="r in rows" :key="r[0]">
          <td><router-link :to="r[0]">열기</router-link></td><td><b>{{ r[1] }}</b></td><td>{{ r[2] }}</td><td>{{ r[3] }}</td><td>{{ r[4] }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
