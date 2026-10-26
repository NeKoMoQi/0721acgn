//判断时间
export const getTime = () => {
  let message = ''
  let hours = new Date().getHours()
  if (hours >= 6 && hours <= 11) {
    message = '早上好'
  } else if (hours > 11 && hours <= 14) {
    message = '中午好'
  } else if (hours > 14 && hours <= 19) {
    message = '下午好'
  } else if (hours > 0 && hours <= 6) {
    message = '凌晨好'
  } else if (hours > 19 && hours < 24) {
    message = '晚上好'
  }
  return message
}
