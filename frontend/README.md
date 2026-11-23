# 0721acgn

This template should help get you started developing with Vue 3 in Vite.

## Recommended IDE Setup

[VSCode](https://code.visualstudio.com/) + [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) (and disable Vetur).

## Type Support for `.vue` Imports in TS

TypeScript cannot handle type information for `.vue` imports by default, so we replace the `tsc` CLI with `vue-tsc` for type checking. In editors, we need [Volar](https://marketplace.visualstudio.com/items?itemName=Vue.volar) to make the TypeScript language service aware of `.vue` types.

## Customize configuration

See [Vite Configuration Reference](https://vite.dev/config/).

## Project Setup

```sh
npm install
```

### Compile and Hot-Reload for Development

```sh
npm run dev
```

### Type-Check, Compile and Minify for Production

```sh
npm run build
```

### Run Unit Tests with [Vitest](https://vitest.dev/)

```sh
npm run test:unit
```
## 站内战棋

网站导航「战棋」进入 `/iron-and-snow/start.html`。`/#/warflag`、`/#/warflag/battle` 和 `/#/warflag/preview` 均兼容旧链接。新版使用当前浏览器的本地存档，旧账号战局与新版进度独立。

`public/iron-and-snow/` 来自《铁与雪》提交 `0bf2a6d` 的生产构建。更新时在独立项目执行 `npm run build`，替换此目录的静态产物，并保留 `SourceHanSans-LICENSE.txt`。封面在此路径下会显示返回网站的链接。主站构建会原样复制此目录。
