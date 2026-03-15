<template>
	<view class="chat-view">
		<scroll-view 
			scroll-y 
			class="chat-main" 
			:scroll-into-view="scrollToMsgId" 
			scroll-with-animation
			@scrolltolower="loadMore"
		>
			<view class="chat-content">
				<view
					v-for="(msg, index) in messages"
					:key="index"
					:id="'msg-' + index"
				>
					<ChatMessage
						:role="msg.role"
						:type="msg.type"
						:content="msg.content"
						:image="msg.image"
					>
						<!-- 插槽：用于扩展特殊消息类型 -->
						<slot :name="'message-' + index" :msg="msg" :index="index"></slot>
					</ChatMessage>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script>
import ChatMessage from '../components/ChatMessage.vue';

export default {
	name: 'ChatView',
	components: {
		ChatMessage
	},
	props: {
		messages: {
			type: Array,
			default: () => []
		},
		scrollToMsgId: {
			type: String,
			default: ''
		},
		loading: {
			type: Boolean,
			default: false
		}
	},
	methods: {
		loadMore() {
			this.$emit('load-more');
		},
		scrollToBottom() {
			if (this.messages.length > 0) {
				this.$emit('scroll-to', 'msg-' + (this.messages.length - 1));
			}
		}
	}
}
</script>

<style lang="scss" scoped>
.chat-view {
	flex: 1;
	height: 0;
	overflow: hidden;
}

.chat-main {
	height: 100%;
	
	.chat-content {
		padding: 40rpx 30rpx;
		padding-bottom: 60rpx;
	}
}
</style>
