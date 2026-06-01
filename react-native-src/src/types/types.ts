export type TaskStatus = 'pending' | 'completed' | 'moved';

export interface Category {
  id: number;
  name: string;
  nameVi: string;
  color: string; // Pastel HEX colors like '#E0BBE4', '#FFD1DC'
  icon: string;  // Icon name from material icons or Feather (e.g., 'briefcase', 'book-open')
  emoji: string; // Playful emoji icon like 💼, 📝, 🏠
}

export interface Task {
  id: string; // Unique string representation (UUID)
  title: string;
  description: string;
  categoryId: number; // Linked with Category.id
  status: TaskStatus;
  creationWeek: number; // Track which week of year this task belongs (1-52)
  scheduledDate: string; // 'YYYY-MM-DD'
  reminderTime: string; // 'HH:mm'
  originalDate?: string; // Tracking the original scheduled date if snooze / postpond
  proofImage?: string; // Lưu đường dẫn URI của ảnh sau khi chụp check-in (ví dụ: 'file://...')
  completedAt?: string; // Lưu chuỗi thời gian (HH:mm) lúc người dùng bấm hoàn thành để hiển thị đè lên ảnh
}

export interface MonthlyGoal {
  id: string;
  title: string;
  completed: boolean;
  targetMonth: string; // 'YYYY-MM'
}
