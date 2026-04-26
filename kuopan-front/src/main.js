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
import NoData from "@/components/NoData.vue"
import FolderSelect from "@/components/FolderSelect.vue"
import Navigation from "@/components/Navigation.vue"
import Preview from "@/components/preview/Preview.vue"
import Window from "@/components/Window.vue"
// import custom utilities
import Verify from '@/utils/Verify'
import Message from '@/utils/Message'
import Request from '@/utils/Request'
import Confirm from '@/utils/Confirm'
import Utils from '@/utils/Utils.js'
// import highlight
import HljsVuePlugin from '@highlightjs/vue-plugin';
import 'highlight.js/styles/atom-one-light.css'
import 'highlight.js/lib/common'

const app = createApp(App)

app.use(router)
app.use(ElementPlus)
app.use(HljsVuePlugin)

app.component("Dialog", Dialog)
app.component("Table", Table)
app.component("Icon", Icon)
app.component("NoData", NoData)
app.component("FolderSelect", FolderSelect)
app.component("Navigation", Navigation)
app.component("Preview", Preview)
app.component("Window", Window)

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
