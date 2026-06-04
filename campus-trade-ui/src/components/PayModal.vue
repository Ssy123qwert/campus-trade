<template>
  <div class="modal-overlay" v-if="visible" @click.self="$emit('close')">
    <div class="modal-body">
      <div class="modal-title">{{ step === 1 ? '确认支付' : step === 2 ? '输入支付密码' : '支付成功' }}</div>

      <!-- 步骤1：确认订单 -->
      <div v-if="step === 1" class="step-confirm">
        <div class="order-info">
          <div class="order-row"><span>商品</span><span>{{ productTitle }}</span></div>
          <div class="order-row"><span>金额</span><span class="amount">&yen;{{ amount }}</span></div>
          <div class="order-row"><span>卖家</span><span>{{ sellerName }}</span></div>
        </div>
        <button class="pay-btn" @click="step = 2">确认支付</button>
        <button class="cancel-btn" @click="$emit('close')">取消</button>
      </div>

      <!-- 步骤2：输入密码（假密码框） -->
      <div v-if="step === 2" class="step-password">
        <p class="hint">请输入6位支付密码（任意6位数字）</p>
        <div class="pwd-boxes">
          <input v-for="(_, i) in 6" :key="i" :ref="el => pwdRefs[i] = el"
                 v-model="pwdDigits[i]" maxlength="1" type="tel"
                 @input="onPwdInput(i)" @keydown.delete="onPwdDelete(i)" />
        </div>
        <button class="pay-btn" :disabled="pwdDigits.join('').length < 6" @click="doPay">立即支付</button>
        <button class="cancel-btn" @click="step = 1">返回</button>
      </div>

      <!-- 步骤3：支付成功 -->
      <div v-if="step === 3" class="step-success">
        <div class="success-icon">✓</div>
        <p>支付成功！</p>
        <p class="sub-text">已通知卖家发货</p>
        <button class="pay-btn" @click="$emit('success')">完成</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, nextTick, watch } from 'vue'

export default {
  name: 'PayModal',
  props: {
    visible: { type: Boolean, default: false },
    amount: { type: Number, default: 0 },
    productTitle: { type: String, default: '' },
    sellerName: { type: String, default: '' }
  },
  emits: ['close', 'success'],
  setup(props, { emit }) {
    const step = ref(1)
    const pwdDigits = ref(['', '', '', '', '', ''])
    const pwdRefs = ref([])

    const resetPwd = () => {
      pwdDigits.value = ['', '', '', '', '', '']
    }

    watch(() => props.visible, (val) => {
      if (val) {
        step.value = 1
        resetPwd()
      }
    })

    const onPwdInput = (i) => {
      // 只保留数字
      pwdDigits.value[i] = pwdDigits.value[i].replace(/\D/g, '')
      if (pwdDigits.value[i] && i < 5) {
        nextTick(() => pwdRefs.value[i + 1]?.focus())
      }
    }

    const onPwdDelete = (i) => {
      if (!pwdDigits.value[i] && i > 0) {
        nextTick(() => pwdRefs.value[i - 1]?.focus())
      }
    }

    const doPay = () => {
      if (pwdDigits.value.join('').length < 6) return
      // 模拟支付处理
      setTimeout(() => {
        step.value = 3
      }, 800)
    }

    return { step, pwdDigits, pwdRefs, onPwdInput, onPwdDelete, doPay }
  }
}
</script>

<style scoped>
.modal-overlay { position: fixed; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); z-index: 200; display: flex; align-items: center; justify-content: center; }
.modal-body { background: #fff; border-radius: 16px; width: 85%; max-width: 360px; padding: 24px; text-align: center; }
.modal-title { font-size: 18px; font-weight: bold; margin-bottom: 20px; color: #333; }

.order-info { background: #f9f9f9; border-radius: 10px; padding: 15px; margin-bottom: 20px; }
.order-row { display: flex; justify-content: space-between; padding: 8px 0; font-size: 14px; color: #666; }
.order-row .amount { color: #f44; font-size: 18px; font-weight: bold; }

.pay-btn { width: 100%; padding: 12px; background: #07c160; color: #fff; border: none; border-radius: 10px; font-size: 16px; cursor: pointer; margin-bottom: 8px; }
.pay-btn:disabled { background: #ccc; }
.cancel-btn { width: 100%; padding: 12px; background: #fff; color: #999; border: 1px solid #ddd; border-radius: 10px; font-size: 14px; cursor: pointer; }

.hint { font-size: 13px; color: #999; margin-bottom: 16px; }
.pwd-boxes { display: flex; justify-content: center; gap: 10px; margin-bottom: 20px; }
.pwd-boxes input { width: 40px; height: 48px; border: 2px solid #ddd; border-radius: 8px; text-align: center; font-size: 22px; outline: none; }
.pwd-boxes input:focus { border-color: #07c160; }

.success-icon { width: 60px; height: 60px; background: #07c160; color: #fff; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 30px; margin: 10px auto 16px; }
.success-icon + p { font-size: 18px; font-weight: bold; color: #333; margin-bottom: 6px; }
.sub-text { font-size: 13px; color: #999; margin-bottom: 20px; }
</style>
