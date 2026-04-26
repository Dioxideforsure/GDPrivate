<template>
  <div>
    <div class="top-panel">
      <el-form
          :model="searchFormData"
          :rules="rules"
          ref="formDataRef"
          label-width="80px"
          @submit.prevent>
        <el-row>
          <el-col :span="4">
            <el-form-item label="用户昵称">
              <el-input clearable placeholder="支持模糊搜索" v-model.trim="searchFormData.userNameFuzzy"
                        @keyup.enter="loadDataList"></el-input>
            </el-form-item>
          </el-col>

          <el-col :span="4">
            <el-form-item label="状态">
              <el-select clearable placeholder="请选择状态" v-model="searchFormData.status">
                <el-option :value="1" label="启用"></el-option>
                <el-option :value="0" label="禁用"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="4" :style="{'padding-left':'10px'}">
            <el-button type="primary" @click="loadDataList">查询</el-button>
          </el-col>
        </el-row>
      </el-form>
    </div>
    <div class="file-list">
      <Table ref="dataTableRef"
             :columns="columns"
             :showPagination="true"
             :dataSource="tableData"
             :fetch="loadDataList"
             :initFetch="true"
             :options="tableOptions">
        <template #space="{index, row}">
          {{ proxy.Utils.size2Str(row.occuSpace) }}/{{ proxy.Utils.size2Str(row.totalSpace) }}
        </template>
        <template #role="{index, row}" style="color: #05a1f5">
          <span v-if="row.role == 0">管理员</span>
          <span v-if="row.role == 1">教师</span>
          <span v-if="row.role == 2">学生</span>
        </template>
        <template #status="{index, row}">
          <span v-if="row.status == true" style="color: #529b2e">启用</span>
          <span v-if="row.status == false" style="color: #f56c62">禁用</span>
        </template>
        <template #op="{index, row}">
          <span class="a-link" @click="updateSpace(row)">分配空间</span>
          <el-divider direction="vertical"></el-divider>
          <span class="a-link" @click="updateUserStatus(row)">{{ row.status == false ? "启用" : "禁用" }}</span>
        </template>
      </Table>
    </div>
    <Dialog
        :show="dialogConfig.show"
        :title="dialogConfig.title"
        :buttons="dialogConfig.buttons"
        width="500px"
        :showCancel="false"
        @close="dialogConfig.show = false">
      <el-form
          :model="formData"
          :rules="rules"
          ref="formDataRef"
          label-width="80px"
          @submit.prevent>
        <el-form-item label="用户昵称">
          {{ formData.userName }}
        </el-form-item>
        <el-form-item label="空间大小" prop="changeSpace">
          <el-input clearable placeholder="请输入空间大小" v-model.trim="formData.changeSpace">
            <template #suffix>MB</template>
          </el-input>
        </el-form-item>
      </el-form>
    </Dialog>
  </div>
</template>

<script setup>
import {getCurrentInstance, nextTick, onMounted, ref} from "vue";
import Dialog from "@/components/Dialog.vue";

const {proxy} = getCurrentInstance();

const api = {
  loadUserList: "/admin/loadUserList",
  updateUserStatus: "/admin/updateUserStatus",
  updateUserSpace: "/admin/updateUserSpace"
}

const dialogConfig = ref({
  show: false,
  title: "修改空间大小",
  buttons: [
    {
      type: "primary",
      text: "确定",
      click: (e) => {
        submitForm();
      },

    },

  ]
})

const formData = ref({});
const formDataRef = ref();
const rules = ref({
  changeSpace: [{required: true, message: "请输入空间大小"}]
});
const tableData = ref({});
const tableOptions = {
  exHeight: 20
};

const updateSpace = (data) => {
  dialogConfig.value.show = true;
  nextTick(() => {
    formDataRef.value.resetFields();
    formData.value = Object.assign({}, data);
  })
};

const submitForm = () => {
  formDataRef.value.validate(async (valid) => {
    if (!valid) {
      return;
    }
    let params = {};
    Object.assign(params, formData.value);
    let result = await proxy.Request({
      url: api.updateUserSpace,
      params: params
    });
    if (!result) {
      return;
    }
    dialogConfig.value.show = false;
    proxy.Message.success("操作成功");
    await loadDataList();
  })
}

const columns = [
  {
    label: "昵称",
    prop: "userName",
    width: 200,
  },
  {
    label: "邮箱",
    prop: "email",
  },
  {
    label: "空间使用",
    prop: "occuSpace",
  },
  {
    label: "总空间",
    prop: "totalSpace",
  },
  {
    label: "加入时间",
    prop: "regTime",
  },
  {
    label: "身份",
    prop: "role",
    scopedSlots: "role"
  },
  {
    label: "状态",
    prop: "status",
    scopedSlots: "status"
  },
  {
    label: "操作",
    prop: "op",
    width: 150,
    scopedSlots: "op"
  },

];

const searchFormData = ref({});


const loadDataList = async () => {
  let params = {
    pageNo: tableData.value.pageNo,
    pageSize: tableData.value.pageSize,
  };
  Object.assign(params, searchFormData.value)
  let result = await proxy.Request({
    url: api.loadUserList,
    params: params
  });
  if (!result) {
    return;
  }
  tableData.value = result.data;
};

// Update user status
const updateUserStatus = (row) => {
  proxy.Confirm(`你确定要【${row.status == false ? "启用" : "禁用"}】吗`,
      async () => {
        let params = {
          userId: row.userId,
          status: row.status == false ? true : false,
        };
        Object.assign(params, searchFormData.value)
        let result = await proxy.Request({
          url: api.updateUserStatus,
          params: params
        });
        if (!result) {
          return;
        }
        await loadDataList();
      });
}

</script>

<style lang="scss" scoped>
.top-panel {
  margin-top: 10px;
}

.avatar {
  width: 50px;
  height: 50px;
  border-radius: 25px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
  }
}
</style>