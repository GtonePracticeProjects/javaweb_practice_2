<script setup>
import { reactive, ref } from 'vue'
import CodeCard from '../../components/CodeCard.vue'
import { http } from '../../api/http'

const cond = reactive({ type: '', from: '', to: '', keyword: '' })
const progress = ref(null)
const msg = reactive({ ok: '', err: '' })

function filename(header) {
  // Content-Disposition: attachment; filename*=UTF-8''transactions.csv
  const m = /filename\*=UTF-8''([^;]+)/i.exec(header || '') || /filename="?([^";]+)"?/i.exec(header || '')
  return m ? decodeURIComponent(m[1]) : 'download.csv'
}

async function download() {
  msg.ok = msg.err = ''
  progress.value = 0
  try {
    const res = await http.get('/transactions/export', {
      params: Object.fromEntries(Object.entries(cond).filter(([, v]) => v)),
      responseType: 'blob',                                     // keep the bytes, do not parse as JSON
      onDownloadProgress: (e) => (progress.value = e.total ? Math.round((e.loaded / e.total) * 100) : e.loaded)
    })
    const url = URL.createObjectURL(res.data)                   // temporary URL that points to the blob
    const a = document.createElement('a')
    a.href = url
    a.download = filename(res.headers['content-disposition'])
    a.click()
    URL.revokeObjectURL(url)                                    // free the memory
    msg.ok = a.download + ' 저장 요청 완료 (' + res.data.size + ' bytes)'
  } catch (e) {
    // with responseType 'blob' an error body is also a Blob -> read it as text to get the message
    const text = e.response?.data instanceof Blob ? await e.response.data.text() : ''
    msg.err = (text && JSON.parse(text).message) || e.message
  } finally {
    progress.value = null
  }
}

const code = `const res = await http.get('/transactions/export', { params, responseType: 'blob' })
const url = URL.createObjectURL(res.data)
const a = document.createElement('a')
a.href = url
a.download = 'transactions.csv'
a.click()
URL.revokeObjectURL(url)

// 가장 단순한 방법: <a href="/api/transactions/export"> (쿠키 인증이면 동작,
// 하지만 Authorization 헤더/에러 처리/진행률은 불가)`
</script>

<template>
  <CodeCard title="⑧ 파일 다운로드 (Blob)" desc="로그인이 필요한 API 가 만든 CSV 를 내려받습니다. 서버는 수식 주입을 막고(=,+,-,@ 로 시작하는 셀에 ' 추가) 엑셀용 BOM 을 붙입니다." :code="code">
    <div class="row">
      <div><label>구분</label>
        <select v-model="cond.type"><option value="">전체</option><option>DEPOSIT</option><option>WITHDRAW</option><option>TRANSFER_IN</option><option>TRANSFER_OUT</option></select></div>
      <div><label>시작일</label><input v-model="cond.from" type="date" /></div>
      <div><label>종료일</label><input v-model="cond.to" type="date" /></div>
      <div><label>메모 / 상대계좌</label><input v-model="cond.keyword" /></div>
    </div>
    <p>
      <button :disabled="progress !== null" @click="download">CSV 다운로드 (axios + Blob)</button>
      <a class="plain-link" href="/api/transactions/export">단순 링크로 받기 (&lt;a href&gt;)</a>
    </p>
    <div v-if="progress !== null" class="msg">다운로드 중… {{ progress }}</div>
    <div v-if="msg.ok" class="msg ok">{{ msg.ok }}</div>
    <div v-if="msg.err" class="msg err">{{ msg.err }}</div>
  </CodeCard>
</template>
