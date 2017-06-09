## IMUI for React Native

## 安装

```
npm install aurora-imui-react-native --save
react-native link
```

如果 link 安卓失败，需要手动修改一下 `settings.gradle` 中的引用路径：

```
include ':app', ':aurora-imui-react-native'
project(':aurora-imui-react-native').projectDir = new File(rootProject.projectDir, '../node_modules/aurora-imui-react-native/ReactNative/android')

```

然后在 app 的 `build.gradle`中引用：

```
dependencies {
    compile project(':aurora-imui-react-native')
}
```



## 配置

- ### Android

  - 引入 Package:

  > MainApplication.java

  ```
  @Override
  protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
          new ReactIMUIPackage()
      );
  }
  ```

  - import IMUI from 'aurora-imui-react-native';





- ### iOS

  - PROJECT -> TARGETS -> Build Settings -> Enable Bitcode Set to No
  - Find PROJECT -> TARGETS -> General -> Embedded Binaries  and add RCTAuroraIMUI.framework
  - 构建你的项目之前，你需要构建 RCTAuroraIMUI.framework

- ## 用法
```
import {
  NativeModules,
} from 'react-native';

import IMUI from 'aurora-imui-react-native';
var MessageList = IMUI.MessageList;
var ChatInput = IMUI.ChatInput;
const AuroraIMUIModule = NativeModules.AuroraIMUIModule;// the IMUI controller, use it to add message to messageList.

// render() 中加入视图标签
<MessageListView />
<InputView />
```
详情可以参考 iOS Android 示例
> [Android Example 用法](./sample/react-native-android/pages/chat_activity.js)
> [iOS Example usage](./sample/index.ios.js)
## 数据格式

使用 MessageList，你需要定义 `message` 对象和 `fromUser` 对象。

- `message` 对象格式:

**status 必须为以下四个值之一: "send_succeed", "send_failed", "send_going", "download_failed"，如果没有定义这个属性， 默认值是 "send_succeed".**

 ```
  message = {  // text message
    msgId: "msgid",
    status: "send_going",
    msgType: "text",
    isOutgoing: true,
    text: "text"
    fromUser: {}
}

message = {  // image message
    msgId: "msgid",
    msgType: "image",
    isOutGoing: true,
    progress: "progress string"
    mediaPath: "image path"
    fromUser: {}
}


message = {  // voice message
    msgId: "msgid",
    msgType: "voice",
    isOutGoing: true,
    duration: number
    mediaPath: "voice path"
    fromUser: {}
}

message = {  // video message
    msgId: "msgid",
    status: "send_failed",
    msgType: "video",
    isOutGoing: true,
    druation: number
    mediaPath: "voice path"
    fromUser: {}
}
 ```

-    `fromUser` 对象格式:

  ```
  fromUser = {
    userId: ""
    displayName: ""
    avatarPath: "avatar image path"
  }
  ```


  ## 事件处理

  ### MessageList 事件
- onAvatarClick {message: {message json}} : 点击头像触发

- onMsgClick {message: {message json} :  点击消息气泡触发

- onStatusViewClick {message: {message json}}  点击消息状态按钮触发

- onPullToRefresh  滚动 MessageList 到顶部时，下拉触发, 案例用法: 参考 sample 中的聊天组件中的 onPullToRefresh  方法。


- onBeginDragMessageList (iOS only) 用于调整布局

  ### MessageList append/update/insert 消息事件:

  插入，更新，增加消息到 MessageList, 你需要使用 AuroraIMUIModule (Native Module) 来发送事件到 Native。

- appendMessages([message])

 example:

```
var messages = [{
	msgId: "1",
	status: "send_going",
	msgType: "text",
	text: "Hello world",
	isOutgoing: true,
	fromUser: {
		userId: "1",
		displayName: "Ken",
		avatarPath: "ironman"
	},
	timeString: "10:00",
}];
AuroraIMUIModule.appendMessages(messages);
```

- updateMessage(message)

example:

```
var message = {
	msgId: "1",
	status: "send_going",
	msgType: "text",
	text: text,
	isOutgoing: true,
	fromUser: {
		userId: "1",
		displayName: "Ken",
		avatarPath: "ironman"
	},
	timeString: "10:00",
};
AuroraIMUIModule.updateMessage(message);
```

- insertMessagesToTop([message])

  **消息数组的顺序排列要按照时间顺序排列**

example:

```
var messages = [{
    msgId: "1",
    status: "send_succeed",
    msgType: "text",
    text: "This",
    isOutgoing: true,
    fromUser: {
	  userId: "1",
	  displayName: "Ken",
	  avatarPath: "ironman"
    },
    timeString: "10:00",
  },{
    msgId: "2",
	status: "send_succeed",
	msgType: "text",
	text: "is",
	isOutgoing: true,
	fromUser: {
		userId: "1",
		displayName: "Ken",
		avatarPath: "ironman"
    },
    timeString: "10:10",
},{
    msgId: "3",
	status: "send_succeed",
	msgType: "text",
	text: "example",
	isOutgoing: true,
	fromUser: {
		userId: "1",
		displayName: "Ken",
		avatarPath: "ironman"
    },
    timeString: "10:20",
}];
AuroraIMUIModule.insertMessagesToTop(messages);
```

## 样式 

### MessageList 自定义样式

**在 Android 中，如果你想要自定义消息气泡，你需要将一张点九图放在 drawable 文件夹下。 [点九图介绍](https://developer.android.com/reference/android/graphics/drawable/NinePatchDrawable.html)，详情参考 sample。**
- sendBubble: PropTypes.string -- 点九图的名字(Android only)


- receiveBubble: PropTypes.string — 同上(Android only)

- sendBubbleTextColor: PropTypes.string,

- receiveBubbleTextColor: PropTypes.string,

- sendBubbleTextSize: PropTypes.number,

- receiveBubbleTextSize: PropTypes.number,


padding 对象包括四个属性: left, top, right, bottom. eg: {left: 5, top: 5, right: 15, bottom: 5}

- sendBubblePadding: PropTypes.object

- receiveBubblePadding: PropTypes.object

- dateTextSize: PropTypes.number,

- dateTextColor: PropTypes.string,

- datePadding: PropTypes.number -- 与上面的不同，这个属性内边距是一样的


- avatarSize: PropTypes.object -- 这个对象有宽高两个属性，Example: avatarSize = {width: 50, height: 50}

- showDisplayName: PropTypes.bool, 
