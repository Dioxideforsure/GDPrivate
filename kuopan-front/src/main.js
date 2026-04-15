import { createApp } from 'vue'
import App from './App.vue'

// import Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// import icons from assets
import '@/assets/icon/iconfont.css'
import '@/assets/icon/base.scss'

// import Cookies
import VueCookies from 'vue-cookies'
import router from "@/router/index.js";

// import custom component
import Dialog from "@/components/Dialog.vue";
import Table from "@/components/Table.vue"
import Icon from "@/components/Icon.vue"
// import custom utilities
import Verify from '@/utils/Verify'
import Message from '@/utils/Message'
import Request from '@/utils/Request'
import Confirm from '@/utils/Confirm'
import Utils from '@/utils/Utils.js'

const app = createApp(App)

app.use(router)
app.use(ElementPlus)

app.component("Dialog", Dialog)
app.component("Table", Table)
app.component("Icon", Icon)

// config global component
app.config.globalProperties.Verify = Verify
app.config.globalProperties.Message = Message
app.config.globalProperties.Request = Request
app.config.globalProperties.Confirm = Confirm
app.config.globalProperties.VueCookies = VueCookies
app.config.globalProperties.Utils = Utils
app.config.globalProperties.globalInfo = {
    imageUrl: "/api/file/getImage/"
}
app.mount('#app')
