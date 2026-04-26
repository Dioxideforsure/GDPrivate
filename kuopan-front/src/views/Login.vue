<template>
  <div class="login-body">
    <div class="bg"></div>
    <div class="login-panel">
      <!--input-->
      <el-form
          class="login-register"
          :model="formData"
          :rules="rules"
          ref="formDataRef"
          @submit.prevent>
        <div class="login-title">KuoPan</div>
        <el-form-item prop="email">
          <el-input size="large" clearable placeholder="请输入邮箱" v-model.trim="formData.email" maxlength="150">
            <template #prefix>
              <span class="iconfont icon-account"></span>
            </template>
          </el-input>
        </el-form-item>
        <!--Login password-->
        <el-form-item prop="password" v-if="opType === 0">
          <el-input
              type="password"
              size="large"
              clearable
              placeholder="请输入密码"
              v-model.trim="formData.password"
              show-password
          >
            <template #prefix>
              <span class="iconfont icon-password"></span>
            </template>
          </el-input>


        </el-form-item>

        <!--Forget my password-->
        <div v-if="opType === 1">
          <div class="send-email-panel">
            <el-form-item prop="emailCode">
              <el-input
                  clearable
                  size="large"
                  placeholder="请输入邮箱验证码"
                  v-model.trim="formData.emailCode">
                <template #prefix>
                  <span class="iconfont icon-checkcode"></span>
                </template>
              </el-input>
            </el-form-item>
            <el-button class="send-mail-btn" type="primary" size="large" @click="getEmailCode">
              获取验证码
            </el-button>

          </div>

          <el-popover placement="left" :width="300" trigger="click">
            <div>
              <p>1.去垃圾箱中寻找验证码</p>
              <p>2.与你的管理员联系</p>
            </div>
            <template #reference>
              <span class="a-link" :style="{ 'font-size' : '14px' }">
                未收到邮箱验证码？
              </span>

            </template>
          </el-popover>

          <!--Find password column-->
          <el-form-item prop="resetPassword" :rules="rules">
            <el-input
                type="password"
                size="large"
                placeholder="请输入密码"
                v-model.trim="formData.resetPassword"
                show-password>
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
          </el-form-item>

          <!--Retype password-->
          <el-form-item prop="resetPasswordConfirm" :rules="rules">
            <el-input
                type="password"
                size="large"
                placeholder="请再次输入密码"
                v-model.trim="formData.resetPasswordConfirm"
                show-password>
              <template #prefix>
                <span class="iconfont icon-password"></span>
              </template>
            </el-input>
          </el-form-item>
        </div>

        <!--Verification-->
        <el-form-item prop="checkCode">
          <div class="check-code">
            <el-input
                size="large"
                placeholder="请输入验证码"
                v-model.trim="formData.checkCode"
            >
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
            </el-input>
          </div>
          <img :src="checkCodeUrl" class="check-code" @click="changeCheckCode(0)"/>
        </el-form-item>

        <!--Back to login-->
        <el-form-item v-if="opType==1">
          <div class="op-panel">

            <div class="no-account">
              <a href="javascript:void(0)" class="a-link" @click="showPanel(0)">去登录？</a>
            </div>
          </div>
        </el-form-item>

        <!--Remember me & forget password-->
        <el-form-item v-if="opType==0">
          <div class="op-panel">
            <div class="rememberme-panel">
              <el-checkbox v-model="formData.rememberMe" v-if="opType == 0">记住我</el-checkbox>
            </div>
            <div class="no-account">
              <a href="javascript:void(0)" class="a-link" @click="showPanel(1)">忘记密码？</a>
            </div>
          </div>
        </el-form-item>
        <!--Login Button-->
        <el-form-item>
          <el-button type="primary" class="op-btn" size="large" v-if="opType==0" @click="doSubmit">
            <span>登录</span>
          </el-button>

          <el-button type="primary" class="op-btn" size="large" v-if="opType==1" @click="doSubmit">
            <span>重置密码</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    <Dialog
        :show="dialogConfig4SendMailCode.show"
        :title="dialogConfig4SendMailCode.title"
        :buttons="dialogConfig4SendMailCode.buttons"
        width="500px"
        :showCancel="false"
        @close="dialogConfig4SendMailCode.show = false">
      <el-form
          :model="formData4SendMailCode"
          :rules="rules"
          ref="formData4SendMailCodeRef"
          label-width="80px"
          @submit.prevent>
        <el-form-item label="邮箱">
          {{ formData.email }}
        </el-form-item>
        <el-form-item label="验证码" prop="checkCode">
          <div class="check-code-panel">
            <el-input
                size="large"
                placeholder="请输入验证码"
                v-model.trim="formData4SendMailCode.checkCode"
            >
              <template #prefix>
                <span class="iconfont icon-checkcode"></span>
              </template>
            </el-input>
            <img :src="checkCodeUrl4SendMailCode" class="check-code" @click="changeCheckCode(1)"/>
          </div>

        </el-form-item>
      </el-form>

    </Dialog>
  </div>
</template>

<script setup>
import {ref, reactive, getCurrentInstance, nextTick} from "vue";
import Dialog from "@/components/Dialog.vue";
import {useRouter, useRoute} from "vue-router";
import sha from "jssha"

const {proxy} = getCurrentInstance();
const router = useRouter()
const route = useRoute()
const api = {
  checkCode: "/api/checkCode",
  sendEmailCode: "/sendEmailCode",
  login: "/login",
  resetPassword: "/resetPassword"
};

// Operation Type 0 login, 1 forget passwd
const opType = ref(0);
const showPanel = (type) => {
  opType.value = type;
  resetForm();
};

const formData = ref({});
const formDataRef = ref();
const checkRePassword = (rule, value, callback) => {
  if (value != formData.value.resetPassword) {
    callback(new Error(rule.message));
  } else {
    callback()
  }
}
const rules = {
  email: [{required: true, message: "请输入正确的邮箱"}, {validator: proxy.Verify.email, message: "请输入正确的邮箱"}],
  password: [{required: true, message: "请输入密码"}],
  emailCode: [{required: true, message: "请输入邮箱密码"}],
  resetPassword: [
    {required: true, message: "请输入密码"},
    {validator: proxy.Verify.password, message: "密码只能是数字，字母，特殊字符8-18位"},
  ],
  resetPasswordConfirm: [
    {required: true, message: "请再次输入密码"},
    {validator: checkRePassword, message: "两次输入的密码不一致"}
  ],
  checkCode: [{required: true, message: "请输入图形验证码"}]

}

const checkCodeUrl = ref(api.checkCode);
const checkCodeUrl4SendMailCode = ref(api.checkCode);

const changeCheckCode = (type) => {
  if (type === 0) {
    checkCodeUrl.value = api.checkCode + "?type=" + type + "&time=" + new Date().getTime();
  } else {
    checkCodeUrl4SendMailCode.value = api.checkCode + "?type=" + type + "&time=" + new Date().getTime();
  }
}

// Send Email Verification code
const formData4SendMailCode = ref({});
const formData4SendMailCodeRef = ref();

const dialogConfig4SendMailCode = reactive({
  show: false,
  title: "发送邮箱验证码",
  buttons: [
    {
      type: "primary",
      text: "发送验证码",
      click: (e) => {
        sendEmailCode();
      }
    }
  ]
})
const getEmailCode = () => {
  formDataRef.value.validateField("email", (valid) => {
    if (!valid) {
      return;
    }
    dialogConfig4SendMailCode.show = true;
    nextTick(() => {
      changeCheckCode(1);
      formData4SendMailCodeRef.value.resetFields();
      formData4SendMailCode.value = {
        email: formData.value.email,
        checkCode: ""
      }
    })
  })
}

// Send email code

const sendEmailCode = () => {
  formData4SendMailCodeRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    const params = Object.assign({}, formData4SendMailCode.value);
    params.type = opType.value;
    if (params.type != 1) {
      proxy.Message.warning("仅允许修改密码，你不会来搞事的吧");
      return;
    }
    let result = await proxy.Request({
      url: api.sendEmailCode,
      params: params,
      timeout: 30000,
      errorCallback: () => {
        changeCheckCode(1)
      }
    });

    if (!result) {
      return;
    }
    proxy.Message.success("验证码发送成功，请登录邮箱查看");
    dialogConfig4SendMailCode.show = false;
  })
}

// Reset the form
const resetForm = () => {
  changeCheckCode(0);
  nextTick(() => {
    if (formDataRef.value) {
      formDataRef.value.resetFields();
    }
  });

  formData.value = {
    email: "",
    password: "",
    emailCode: "",
    resetPassword: "",
    resetPasswordConfirm: "",
    checkCode: ""
  };
  if (opType.value === 0) {
    const cookieLoginInfo = proxy.VueCookies.get("loginInfo");
    if (cookieLoginInfo) {
      formData.value = cookieLoginInfo;
    }
  }
}

// Login, Reset password to submit form

const doSubmit = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    let params = {};
    Object.assign(params, formData.value);
    // Reset the password
    if (opType.value == 1) {
      params.password = params.resetPassword;
      delete params.resetPassword;
    }
    // Login
    if (opType.value == 0) {
      let cookieLoginInfo = proxy.VueCookies.get("loginInfo");
      let cookiePassword = cookieLoginInfo == null ? null : cookieLoginInfo.password;
      if (params.password != cookiePassword) {
        const shaObj = new sha("SHA-256", "TEXT", {encoding: "UTF8"})
        shaObj.update(params.password)
        params.password = shaObj.getHash("HEX")
      }
    }
    let url = null;
    if (opType.value == 0) {
      url = api.login
    } else if (opType.value == 1) {
      url = api.resetPassword
    }
    let result = await proxy.Request({
      url: url,
      params: params,
      errorCallback: () => {
        changeCheckCode(0)
      },
    })

    if (!result) {
      return;
    }

    // Return of login and reset password
    // Login
    if (opType.value == 0) {
      if (params.rememberMe) {
        const loginInfo = {
          email: params.email,
          password: params.password,
          rememberMe: params.rememberMe
        };
        proxy.VueCookies.set("loginInfo", loginInfo, "7d");
      } else {
        proxy.VueCookies.remove("loginInfo")
      }
      proxy.Message.success("登录成功")
      // Save into the cookie when browser is on
      proxy.VueCookies.set("userInfo", result.data, 0);
      // Redirect
      const redirectUrl = route.query.redirectUrl || "/"
      router.push(redirectUrl)
    }
    // Reset the password
    else if (opType.value == 1) {
      proxy.Message.success("重置密码成功，请登录");
      // Back to the login page
      showPanel(0);
    }
  });
}
</script>

<style lang="scss" scoped>
.login-body {
  height: calc(100vh);
  background: url("../assets/login_bg.jpg");
  background-size: cover;
  display: flex;

  .bg {
    flex: 1;
    background-image: url("../assets/login_img.png");
    background-position: center;
    background-repeat: no-repeat;
    background-size: 40%;
  }

  .login-panel {
    width: 430px;
    margin-right: 15%;
    margin-top: calc((100vh - 500px) / 2);

    .login-register {
      padding: 25px;
      background: #ffffff;
      border-radius: 5px;

      .login-title {
        text-align: center;
        font-size: 18px;
        font-weight: bold;
        margin-bottom: 20px;
      }

      .send-email-panel {
        display: flex;
        width: 100%;
        justify-content: space-between;

        .send-mail-btn {
          margin-left: 5px;
        }
      }


      .op-panel {
        width: 100%;
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;

        .rememberme-panel {
          flex: none;
          width: auto;
        }

        .no-account {
          flex: none;
          width: auto;
          display: flex;
        }
      }

      .op-btn {
        width: 100%;
      }
    }
  }

  .check-code-panel {
    width: 100%;
    display: flex;

    .check-code {
      margin-left: 5px;
      cursor: pointer;
    }
  }
}
</style>