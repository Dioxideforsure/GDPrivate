<script setup>
import {getCurrentInstance, ref} from "vue";
import {useRoute, useRouter} from "vue-router";

const route = useRoute();
const router = useRouter();
const {proxy} = getCurrentInstance();
const api = {
  getShareInfo: "/showShare/getShareInfo",
  checkShareCode: "/showShare/checkShareCode"
};

const shareId = route.params.shareId;
const shareInfo = ref({});

const formData = ref({});
const formDataRef = ref();
const rules = ref({
  code: [{required: true, message: "请输入提取码"},
    {min: 5, message: "提取码为5位"},
    {max: 5, message: "提取码为5位"}
  ]
});

const checkShare = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    let params = {
      shareId: shareId,
      code: formData.value.code
    };
    Object.assign(params, formData.value);
    let result = await proxy.Request({
      url: api.checkShareCode,
      params: params
    });
    if (!result) {
      return;
    }
    await router.push(`/share/${shareId}`)
  })
}

const getShareInfo = async () => {
  let result = await proxy.Request({
    url: api.getShareInfo,
    params: {
      shareId
    },
  });
  if (!result) {
    return;
  }
  shareInfo.value = result.data;
}

getShareInfo();
</script>

<template>
  <div class="share">
    <div class="body-content">
      <div class="logo">
        <span class="iconfont icon-pan"></span>
        <span class="name">阔盘</span>
      </div>
      <div class="code-panel">
        <div class="file-info">
          <div class="share-info">
            <div class="user-info">
              <span class="nick-name">{{ shareInfo.userName }}</span>
              <span class="share-time">分享于：{{ shareInfo.shareTime }}</span>
            </div>
            <div class="file-name">分享文件：{{ shareInfo.fileName }}
            </div>
          </div>
        </div>
        <div class="code-body">
          <div class="tips">请输入提取码：</div>
          <div class="input-area">
            <el-form
                :model="formData"
                :rules="rules"
                ref="formDataRef"
                @submit.prevent>
              <el-form-item prop="code">
                <el-input
                    class="input"
                    clearable
                    v-model.trim="formData.code"
                    @keyup.enter="checkShare">
                </el-input>
                <el-button type="primary" @click="checkShare">提取文件</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


<style scoped lang="scss">
.share {
  height: calc(100vh);
  background: #eef2f6 url("../../assets/share_bg.jpg") repeat-x 0 bottom;
  display: flex;
  justify-content: center;

  .body-content {
    margin-top: calc(100vh / 5);
    width: 500px;

    .logo {
      display: flex;
      align-items: center;
      justify-content: center;

      .icon-pan {
        font-size: 60px;
        color: #FFD1A4;
      }

      .name {
        font-weight: bold;
        margin-left: 5px;
        font-size: 25px;
        color: #FFD1A4;
      }
    }
  }

  .code-panel {
    margin-top: 20px;
    background: #fff;
    border-radius: 5px;
    overflow: hidden;
    box-shadow: 0 0 7px 1px #5757574f;

    .file-info {
      padding: 10px 20px;
      background: #409eff;
      color: #fff;
      display: flex;
      align-items: center;

      .avatar {
        margin-right: 5px;
      }

      .share-info {
        .user-info {
          display: flex;
          align-items: center;

          .nick-name {
            font-size: 15px;
          }

          .share-time {
            margin-left: 20px;
            font-size: 12px;
          }
        }

        .file-name {
          margin-top: 10px;
          font-size: 12px;
        }
      }
    }

    .code-body {
      padding: 30px 20px 60px 20px;

      .tips {
        font-weight: bold;
      }

      .input-area {
        margin-top: 10px;

        .input {
          flex: 1;
          margin-right: 10px;
        }
      }
    }
  }
}
</style>