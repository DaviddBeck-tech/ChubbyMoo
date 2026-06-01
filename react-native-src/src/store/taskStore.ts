import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Task, MonthlyGoal, TaskStatus } from '../types/types';
import { scheduleTaskNotification, cancelTaskNotification } from '../services/notificationService';

// Categories constant list for global component usage
export const CATEGORIES = [
  { id: 1, name: 'Work', nameVi: 'Công việc', color: '#E0BBE4', emoji: '💼', icon: 'briefcase' },
  { id: 2, name: 'Study', nameVi: 'Học tập', color: '#FFD1DC', emoji: '📝', icon: 'book' },
  { id: 3, name: 'Personal', nameVi: 'Cá nhân', color: '#FFFFD1', emoji: '🏠', icon: 'home' },
  { id: 4, name: 'Health', nameVi: 'Sức khỏe', color: '#B5EAD7', emoji: '🌱', icon: 'heart' },
];

interface TaskState {
  tasks: Task[];
  monthlyGoals: MonthlyGoal[];
  
  // Task state triggers
  addTask: (title: string, description: string, categoryId: number, dateStr: string, timeStr: string) => void;
  toggleTaskComplete: (id: string, proofImage?: string, completedAt?: string) => void;
  postponeTask: (id: string, daysToAdd: number) => void;
  rescheduleTask: (id: string, postponeType: 'tomorrow' | 'next-week') => void;
  deleteTask: (id: string) => void;
  
  // Goals triggers
  addMonthlyGoal: (title: string, targetMonth: string) => void;
  toggleMonthlyGoal: (id: string) => void;
  deleteMonthlyGoal: (id: string) => void;

  // Trợ lý lọc danh sách công việc trong tuần hiện tại (Thứ 2 -> Chủ Nhật)
  getCurrentWeekTasks: () => Task[];
}

// Simple Helper to calculate week of year dynamically (1 to 52)
const getWeekNumber = (date: Date): number => {
  const target = new Date(date.valueOf());
  const dayNr = (date.getDay() + 6) % 7;
  target.setDate(target.getDate() - dayNr + 3);
  const firstThursday = target.valueOf();
  target.setMonth(0, 1);
  if (target.getDay() !== 4) {
    target.setMonth(0, 1 + ((4 - target.getDay() + 7) % 7));
  }
  return 1 + Math.ceil((firstThursday - target.valueOf()) / 604800000);
};

export const useTaskStore = create<TaskState>()(
  persist(
    (set, get) => ({
      tasks: [
        // Seed some happy default data to brighten up the dashboard initially
        {
          id: 'default-task-1',
          title: '🧘‍♀️ Giải tỏa tâm trí bằng thiền',
          description: 'Hít thở sâu trong phòng yên tĩnh và bật nhạc sóng não 15 phút.',
          categoryId: 4, // Health
          status: 'pending',
          creationWeek: getWeekNumber(new Date()),
          scheduledDate: new Date().toISOString().split('T')[0],
          reminderTime: '07:30',
        },
        {
          id: 'default-task-2',
          title: '🍓 Mua trái cây tươi lành mạnh',
          description: 'Mua bơ sáp thơm mềm và hộp dâu tây chín đỏ mọng ở siêu thị sạch.',
          categoryId: 3, // Personal
          status: 'completed',
          creationWeek: getWeekNumber(new Date()),
          scheduledDate: new Date().toISOString().split('T')[0],
          reminderTime: '17:00',
        }
      ],
      monthlyGoals: [
        {
          id: 'goal-1',
          title: 'Thức dậy trước 6h30 cả tháng',
          completed: false,
          targetMonth: new Date().toISOString().slice(0, 7),
        },
        {
          id: 'goal-2',
          title: 'Đọc hết 2 cuốn sách hạt giống tâm hồn',
          completed: true,
          targetMonth: new Date().toISOString().slice(0, 7),
        }
      ],

      // Add a beautifully scheduled task
      addTask: (title, description, categoryId, dateStr, timeStr) => {
        const parsedDate = new Date(dateStr);
        const weekNum = getWeekNumber(parsedDate);
        
        const newTask: Task = {
          id: `task_${Date.now()}_${Math.random().toString(36).substr(2, 5)}`,
          title,
          description,
          categoryId,
          status: 'pending',
          creationWeek: weekNum,
          scheduledDate: dateStr,
          reminderTime: timeStr,
          originalDate: dateStr,
        };

        set((state) => ({
          tasks: [...state.tasks, newTask],
        }));

        // Side-effect: Schedule local push notification
        scheduleTaskNotification(newTask);
      },

      // Toggle task done status
      toggleTaskComplete: (id, proofImage, completedAt) => {
        set((state) => {
          const updatedTasks = state.tasks.map((task) => {
            if (task.id === id) {
              const currentStatus = task.status;
              let nextStatus: TaskStatus = 'completed';
              let updatedProofImage = task.proofImage;
              let updatedCompletedAt = task.completedAt;
              
              if (currentStatus === 'completed') {
                // Return to pending, but check if it was rescheduled before
                nextStatus = task.originalDate !== task.scheduledDate ? 'moved' : 'pending';
                updatedProofImage = undefined;
                updatedCompletedAt = undefined;
                // Side-effect: re-schedule notification
                setTimeout(() => {
                  scheduleTaskNotification({ ...task, status: nextStatus, proofImage: undefined, completedAt: undefined });
                }, 10);
              } else {
                updatedProofImage = proofImage;
                updatedCompletedAt = completedAt;
                // Side-effect: cancel notification since task is completed
                setTimeout(() => {
                  cancelTaskNotification(task.id);
                }, 10);
              }
              return { 
                ...task, 
                status: nextStatus,
                proofImage: updatedProofImage,
                completedAt: updatedCompletedAt
              };
            }
            return task;
          });
          return { tasks: updatedTasks };
        });
      },

      // Reschedule or Snooze task (E.g. Move +1 Day or +7 Days)
      postponeTask: (id, daysToAdd) => {
        set((state) => {
          const updatedTasks = state.tasks.map((task) => {
            if (task.id === id) {
              const currentDate = new Date(task.scheduledDate);
              currentDate.setDate(currentDate.getDate() + daysToAdd);
              
              const newDateStr = currentDate.toISOString().split('T')[0];
              const newWeek = getWeekNumber(currentDate);

              const updatedTask: Task = {
                ...task,
                status: 'moved',
                scheduledDate: newDateStr,
                creationWeek: newWeek,
                originalDate: task.originalDate || task.scheduledDate, // Retain original date
              };

              // Side-effect: rescheduling notification
              setTimeout(() => {
                scheduleTaskNotification(updatedTask);
              }, 10);

              return updatedTask;
            }
            return task;
          });
          return { tasks: updatedTasks };
        });
      },

      // Specific "Dời Lịch" (Reschedule Logic) in Store
      rescheduleTask: (id, postponeType) => {
        set((state) => {
          const updatedTasks = state.tasks.map((task) => {
            if (task.id === id) {
              const currentDate = new Date(task.scheduledDate);
              const nextDate = new Date(currentDate.getTime());

              if (postponeType === 'tomorrow') {
                // Cập nhật scheduledDate = ngày tiếp theo
                nextDate.setDate(nextDate.getDate() + 1);
              } else if (postponeType === 'next-week') {
                // Cập nhật scheduledDate = ngày thứ Hai của tuần kế tiếp
                const day = currentDate.getDay();
                // If day is Sunday (0), next Monday is day + 1. Otherwise, 1 + (7 - day) % 7 or simply:
                const offsetToMonday = (1 - day + 7) % 7 || 7;
                nextDate.setDate(nextDate.getDate() + offsetToMonday);
              }

              const newDateStr = nextDate.toISOString().split('T')[0];
              const newWeek = getWeekNumber(nextDate);

              const updatedTask: Task = {
                ...task,
                status: 'moved',
                scheduledDate: newDateStr,
                creationWeek: newWeek,
                originalDate: task.originalDate || task.scheduledDate,
              };

              // Side-effect: Cancel old notification and setup a new one
              setTimeout(() => {
                scheduleTaskNotification(updatedTask);
              }, 10);

              return updatedTask;
            }
            return task;
          });
          return { tasks: updatedTasks };
        });
      },

      // Remove unwanted task
      deleteTask: (id) => {
        set((state) => ({
          tasks: state.tasks.filter((task) => task.id !== id),
        }));
        
        // Side-effect: cancel scheduled notification
        cancelTaskNotification(id);
      },

      // Create monthly commitment goals
      addMonthlyGoal: (title, targetMonth) => {
        set((state) => ({
          monthlyGoals: [
            ...state.monthlyGoals,
            {
              id: `goal_${Date.now()}`,
              title,
              completed: false,
              targetMonth,
            },
          ],
        }));
      },

      // Toggle monthly goal checked status
      toggleMonthlyGoal: (id) => {
        set((state) => ({
          monthlyGoals: state.monthlyGoals.map((goal) =>
            goal.id === id ? { ...goal, completed: !goal.completed } : goal
          ),
        }));
      },

      // Delete Monthly goal
      deleteMonthlyGoal: (id) => {
        set((state) => ({
          monthlyGoals: state.monthlyGoals.filter((goal) => goal.id !== id),
        }));
      },

      // Hàm lọc danh sách các công việc nằm trong tuần hiện tại (Thứ 2 đến Chủ Nhật)
      getCurrentWeekTasks: () => {
        const tasks = get().tasks;
        const today = new Date();
        const day = today.getDay(); // 0: Chủ Nhật, 1: Thứ 2, ...
        // Tính toán độ lệch ngày để về Thứ 2 đầu tuần (Nếu hôm nay là Chủ Nhật thì lùi 6 ngày)
        const daysToMonday = day === 0 ? -6 : 1 - day;
        
        const monday = new Date(today);
        monday.setDate(today.getDate() + daysToMonday);
        monday.setHours(0, 0, 0, 0);
        
        const sunday = new Date(monday);
        sunday.setDate(monday.getDate() + 6);
        sunday.setHours(23, 59, 59, 999);
        
        const mondayStr = monday.toISOString().split('T')[0];
        const sundayStr = sunday.toISOString().split('T')[0];
        
        return tasks.filter((task) => {
          return task.scheduledDate >= mondayStr && task.scheduledDate <= sundayStr;
        });
      },
    }),
    {
      name: 'lovely-scheduler-storage',
      storage: createJSONStorage(() => AsyncStorage),
    }
  )
);
