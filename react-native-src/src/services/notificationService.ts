import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { Task } from '../types/types';

// Setup default behavior for how notifications show up when the app is in the foreground
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
  }),
});

/**
 * Thấy và yêu cầu quyền gửi thông báo từ người dùng.
 */
export async function requestNotificationPermissions(): Promise<boolean> {
  if (Platform.OS === 'web') return false;
  
  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;
  
  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }
  
  if (finalStatus !== 'granted') {
    console.log('Quyền thông báo bị từ chối rồi! 🥺');
    return false;
  }
  return true;
}

/**
 * Lên lịch một thông báo mới cho một Task dựa trên scheduledDate ('YYYY-MM-DD') và reminderTime ('HH:mm')
 */
export async function scheduleTaskNotification(task: Task): Promise<string | null> {
  try {
    const hasPermission = await requestNotificationPermissions();
    if (!hasPermission) return null;

    // Hủy thông báo cũ nếu có trùng lặp ID trước khi lên lịch mới
    await cancelTaskNotification(task.id);

    // Phân tích cú pháp ngày và giờ tương ứng
    const [year, month, day] = task.scheduledDate.split('-').map(Number);
    const [hour, minute] = task.reminderTime.split(':').map(Number);
    
    const triggerDate = new Date(year, month - 1, day, hour, minute, 0);
    const now = new Date();

    if (triggerDate <= now) {
      console.log(`Thời gian thông báo cho "${task.title}" nằm ở quá khứ, bỏ qua việc tự lên lịch.`);
      return null;
    }

    // Thiết lập thông báo dễ thương bằng expo-notifications
    const notificationId = await Notifications.scheduleNotificationAsync({
      identifier: task.id, // Đặt trùng với ID của task để vô cùng dễ dàng khi cần hủy
      content: {
        title: `🌱 Lovely Scheduler Nhắc Bạn • ${task.reminderTime}`,
        body: `✨ Hãy làm "${task.title}" thôi nào! Bạn sẽ làm xuất sắc thôi! 🥰`,
        sound: true,
        data: { taskId: task.id },
      },
      trigger: triggerDate,
    });

    console.log(`🔔 Đã lên lịch thành công cho task [${task.id}] vào lúc ${triggerDate.toString()}`);
    return notificationId;
  } catch (error) {
    console.error('Lỗi khi lên lịch thông báo:', error);
    return null;
  }
}

/**
 * Hủy thông báo đã được lên lịch dựa trên ID của Task
 */
export async function cancelTaskNotification(taskId: string): Promise<void> {
  try {
    await Notifications.cancelScheduledNotificationAsync(taskId);
    console.log(`🔕 Đã hủy thành công thông báo của task [${taskId}]`);
  } catch (error) {
    console.error(`Lỗi khi hủy thông báo task [${taskId}]:`, error);
  }
}

/**
 * Khi dời lịch một task sang thời điểm mới:
 * Gọi hàm cancel thông báo cũ và thiết lập thông báo mới cho task đó.
 */
export async function rescheduleTaskNotification(task: Task): Promise<string | null> {
  console.log(`🔄 Đang chuẩn bị dời lịch thông báo cho task [${task.id}]...`);
  await cancelTaskNotification(task.id);
  return await scheduleTaskNotification(task);
}
