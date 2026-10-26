//引入项目全部全局组件
import SvgIcon from './SvgIcon/index.vue'
import Pagination from './Pagination/index.vue'
import Behind from './behind/index.vue'
import Headpage from './Headpage/index.vue'
import Acgncomponent from './Acgncomponents/index.vue'

const allGloablComponent = {SvgIcon,Pagination,Behind,Headpage,Acgncomponent}

//对外暴露插件
export default{
    install(app){
        //注册项目全局组件
        Object.keys(allGloablComponent).forEach(key=>{
            //注册全局组件
            app.component(key,allGloablComponent[key])
        })
    }
}