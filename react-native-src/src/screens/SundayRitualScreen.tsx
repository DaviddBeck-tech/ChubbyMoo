import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  TextInput,
  FlatList,
  Alert
} from 'react-native';
import { PALETTE, SPACING, ROUNDED, SHADOWS, TYPOGRAPHY } from '../theme/theme';
import { useTaskStore, CATEGORIES } from '../store/taskStore';

const WEEKDAYS_VI = [
  { key: 'Monday', label: 'Thứ 2', short: 'T2' },
  { key: 'Tuesday', label: 'Thứ 3', short: 'T3' },
  { key: 'Wednesday', label: 'Thứ 4', short: 'T4' },
  { key: 'Thursday', label: 'Thứ 5', short: 'T5' },
  { key: 'Friday', label: 'Thứ 6', short: 'T6' },
  { key: 'Saturday', label: 'Thứ 7', short: 'T7' },
  { key: 'Sunday', label: 'Chủ Nhật', short: 'CN' },
];

export default function SundayRitualScreen({ navigation }: any) {
  const [activeTab, setActiveTab] = useState<'week' | 'month'>('week');
  const [newGoalTitle, setNewGoalTitle] = useState('');
  
  // Connect cleanly to our Zustand store state
  const { tasks, addTask, monthlyGoals, addMonthlyGoal, toggleMonthlyGoal, toggleTaskComplete } = useTaskStore();

  // Simple logic to compute future scheduled dates for the coming week relative to current day
  const getUpcomingDateString = (dayIndex: number): string => {
    const today = new Date();
    const currentDay = today.getDay(); // 0 indicates Sunday, 1 Monday, and so on
    const daysUntilNextWeekDay = ((dayIndex + 1) - currentDay + 7) % 7 || 7;
    const targetDate = new Date(today.getTime() + daysUntilNextWeekDay * 24 * 60 * 60 * 1000);
    return targetDate.toISOString().split('T')[0];
  };

  // Safe handler to quickly inject a standard template task for next week
  const handleQuickAdd = (dayName: string, dateString: string) => {
    Alert.prompt(
      '🌟 Thêm task nhanh',
      `Tạo công việc mới cho ngày ${dayName} (${dateString}):`,
      [
        { text: 'Hủy lượng', style: 'cancel' },
        {
          text: 'Thành công',
          onPress: (taskTitle) => {
            if (taskTitle && taskTitle.trim() !== '') {
              // Map default task to 'Personal' category (ID: 3) with default morning reminder
              addTask(
                taskTitle.trim(),
                `Được lên lịch vào ngày ${dayName} trong nghi thức chuẩn bị Chủ Nhật.`,
                3, // Personal
                dateString,
                '09:00'
              );
            }
          },
        },
      ],
      'plain-text',
      ''
    );
  };

  // Add new monthly milestone commitment
  const handleAddGoal = () => {
    if (!newGoalTitle.trim()) return;
    const currentYearMonth = new Date().toISOString().slice(0, 7); // Active formatting 'YYYY-MM'
    addMonthlyGoal(newGoalTitle.trim(), currentYearMonth);
    setNewGoalTitle('');
  };

  return (
    <View style={styles.container}>
      {/* 🌸 HEADER REMINDER BANNER */}
      <View style={styles.headerBanner}>
        <View style={styles.bannerInfo}>
          <Text style={styles.bannerTitle}>Chào buổi sáng! ☕</Text>
          <Text style={styles.bannerSubtitle}>
            Hôm nay là Chủ Nhật đấy, hãy dọn dẹp tâm trí và chuẩn bị tinh thần cho tuần mới nhé! ✨
          </Text>
        </View>
        <View style={styles.avatarFrame}>
          <View style={styles.avatarInner} />
        </View>
      </View>

      {/* 📑 SWITCHING TABS NAVIGATION */}
      <View style={styles.tabContainer}>
        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'week' && styles.tabButtonActive]}
          onPress={() => setActiveTab('week')}
        >
          <Text style={[styles.tabText, activeTab === 'week' && styles.tabTextActive]}>
            📅 Lịch Tuần Tới
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[styles.tabButton, activeTab === 'month' && styles.tabButtonActive]}
          onPress={() => setActiveTab('month')}
        >
          <Text style={[styles.tabText, activeTab === 'month' && styles.tabTextActive]}>
            🎯 Mục Tiêu Tháng
          </Text>
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.mainScroll} contentContainerStyle={styles.scrollContent}>
        {activeTab === 'week' ? (
          // 🗓️ SECTION 1: UPCOMING WEEK SPREADSHEET
          <View style={styles.sectionContainer}>
            <Text style={styles.sectionTitle}>Nghi thức chuẩn bị cho tuần sau</Text>
            
            {WEEKDAYS_VI.map((day, index) => {
              const targetDateStr = getUpcomingDateString(index);
              
              // Filter out tasks scheduled for this day
              const tasksForDay = tasks.filter((t) => t.scheduledDate === targetDateStr);

              return (
                <View key={day.key} style={styles.dayBoxCard}>
                  <View style={styles.dayHeaderRow}>
                    <View style={styles.badgeLabel}>
                      <Text style={styles.badgeText}>{day.short}</Text>
                    </View>
                    <View style={styles.dayDateInfo}>
                      <Text style={styles.dayLabel}>{day.label}</Text>
                      <Text style={styles.dateLabel}>{targetDateStr}</Text>
                    </View>
                    <TouchableOpacity
                      style={styles.quickAddBtn}
                      onPress={() => handleQuickAdd(day.label, targetDateStr)}
                    >
                      <Text style={styles.quickAddBtnText}>+ Thêm nhanh</Text>
                    </TouchableOpacity>
                  </View>

                  {/* Tasks nested inside this weekday container */}
                  {tasksForDay.length > 0 ? (
                    tasksForDay.map((task) => {
                      const category = CATEGORIES.find((c) => c.id === task.categoryId);
                      const isDone = task.status === 'completed';
                      return (
                        <TouchableOpacity
                          key={task.id}
                          style={[
                            styles.taskMiniCard,
                            { backgroundColor: isDone ? PALETTE.successMint : category?.color || '#FFFFFF' }
                          ]}
                          onPress={() => toggleTaskComplete(task.id)}
                        >
                          <Text style={[styles.taskMiniTitle, isDone && styles.lineThroughText]}>
                            {category?.emoji} {task.title}
                          </Text>
                          <Text style={styles.taskMiniTime}>{task.reminderTime}</Text>
                        </TouchableOpacity>
                      );
                    })
                  ) : (
                    <Text style={styles.noTasksPlaceholder}>Chưa có công việc nào scheduled.</Text>
                  )}
                </View>
              );
            })}
          </View>
        ) : (
          // 🎯 SECTION 2: MONTHLY COMMITMENT MILESTONES
          <View style={styles.sectionContainer}>
            <Text style={styles.sectionTitle}>Các mốc cam kết trong tháng</Text>
            
            {/* Input Form to create monthly targets */}
            <View style={styles.goalFormRow}>
              <TextInput
                style={styles.goalInput}
                placeholder="Nhập mục tiêu lớn tháng này..."
                placeholderTextColor={PALETTE.textLightGray}
                value={newGoalTitle}
                onChangeText={setNewGoalTitle}
              />
              <TouchableOpacity style={styles.goalSubmitButton} onPress={handleAddGoal}>
                <Text style={styles.goalSubmitText}>Thêm</Text>
              </TouchableOpacity>
            </View>

            {monthlyGoals.length > 0 ? (
              monthlyGoals.map((goal) => (
                <TouchableOpacity
                  key={goal.id}
                  style={[
                    styles.goalItemCard,
                    goal.completed ? styles.goalCompletedBg : styles.goalPendingBg
                  ]}
                  onPress={() => toggleMonthlyGoal(goal.id)}
                >
                  <View style={styles.goalLeftCol}>
                    <View style={[styles.checkboxIndicator, goal.completed && styles.checkboxCompleted]}>
                      {goal.completed && <Text style={styles.checkMarkIcon}>✓</Text>}
                    </View>
                    <Text style={[styles.goalItemTitle, goal.completed && styles.lineThroughText]}>
                      {goal.title}
                    </Text>
                  </View>
                  <Text style={styles.goalMonthBadge}>{goal.targetMonth}</Text>
                </TouchableOpacity>
              ))
            ) : (
              <View style={styles.emptyContainer}>
                <Text style={styles.noTasksPlaceholder}>Hãy viết vài cam kết lớn để bứt phá nào! 🔥</Text>
              </View>
            )}
          </View>
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: PALETTE.backgroundCream,
  },
  headerBanner: {
    paddingTop: SPACING.xxl + 8,
    paddingBottom: SPACING.lg,
    paddingHorizontal: SPACING.xl,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    borderBottomLeftRadius: ROUNDED.xxl,
    borderBottomRightRadius: ROUNDED.xxl,
    ...SHADOWS.md,
  },
  bannerInfo: {
    flex: 1,
    paddingRight: SPACING.md,
  },
  bannerTitle: {
    ...TYPOGRAPHY.titleLarge,
    color: PALETTE.textCharcoal,
    marginBottom: SPACING.xs,
  },
  bannerSubtitle: {
    ...TYPOGRAPHY.bodySmall,
    color: PALETTE.textGray,
    lineHeight: 18,
  },
  avatarFrame: {
    width: 48,
    height: 48,
    borderRadius: ROUNDED.lg,
    backgroundColor: PALETTE.secondaryPink,
    borderWidth: 2,
    borderColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    ...SHADOWS.sm,
  },
  avatarInner: {
    width: 24,
    height: 24,
    borderRadius: ROUNDED.full,
    backgroundColor: PALETTE.focusPink,
  },
  tabContainer: {
    flexDirection: 'row',
    margin: SPACING.lg,
    backgroundColor: '#EAEAEA',
    borderRadius: ROUNDED.lg,
    padding: 4,
  },
  tabButton: {
    flex: 1,
    paddingVertical: SPACING.md,
    alignItems: 'center',
    borderRadius: ROUNDED.md,
  },
  tabButtonActive: {
    backgroundColor: '#FFFFFF',
    ...SHADOWS.sm,
  },
  tabText: {
    fontSize: 13,
    fontWeight: '600',
    color: PALETTE.textGray,
  },
  tabTextActive: {
    color: PALETTE.textCharcoal,
    fontWeight: 'bold',
  },
  mainScroll: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 100,
  },
  sectionContainer: {
    paddingHorizontal: SPACING.lg,
  },
  sectionTitle: {
    ...TYPOGRAPHY.titleMedium,
    color: PALETTE.textCharcoal,
    marginBottom: SPACING.md,
    fontWeight: 'bold',
  },
  dayBoxCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: ROUNDED.xxl,
    padding: SPACING.lg,
    marginBottom: SPACING.md,
    borderWidth: 1,
    borderColor: '#EEEEEE',
    ...SHADOWS.sm,
  },
  dayHeaderRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: SPACING.sm,
  },
  badgeLabel: {
    backgroundColor: PALETTE.primaryLavender,
    borderRadius: ROUNDED.md,
    paddingHorizontal: 10,
    paddingVertical: 6,
    marginRight: SPACING.md,
  },
  badgeText: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  dayDateInfo: {
    flex: 1,
  },
  dayLabel: {
    fontSize: 14,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  dateLabel: {
    fontSize: 11,
    color: PALETTE.textLightGray,
  },
  quickAddBtn: {
    backgroundColor: PALETTE.secondaryPink,
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: ROUNDED.full,
    ...SHADOWS.sm,
  },
  quickAddBtnText: {
    fontSize: 11,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  taskMiniCard: {
    padding: SPACING.md,
    borderRadius: ROUNDED.md,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: SPACING.sm,
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.05)',
  },
  taskMiniTitle: {
    fontSize: 13,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  taskMiniTime: {
    fontSize: 11,
    fontWeight: 'semibold',
    color: PALETTE.textGray,
  },
  lineThroughText: {
    textDecorationLine: 'line-through',
    opacity: 0.5,
  },
  noTasksPlaceholder: {
    fontSize: 12,
    fontStyle: 'italic',
    color: PALETTE.textLightGray,
    textAlign: 'center',
    marginTop: SPACING.sm,
  },
  goalFormRow: {
    flexDirection: 'row',
    backgroundColor: '#FFFFFF',
    borderRadius: ROUNDED.xl,
    padding: 6,
    marginBottom: SPACING.lg,
    borderWidth: 1,
    borderColor: '#EEEEEE',
  },
  goalInput: {
    flex: 1,
    paddingHorizontal: SPACING.md,
    fontSize: 14,
    color: PALETTE.textCharcoal,
  },
  goalSubmitButton: {
    backgroundColor: PALETTE.primaryLavender,
    paddingHorizontal: 16,
    paddingVertical: 10,
    borderRadius: ROUNDED.lg,
    justifyContent: 'center',
  },
  goalSubmitText: {
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  goalItemCard: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: SPACING.lg,
    borderRadius: ROUNDED.xl,
    marginBottom: SPACING.sm,
    borderWidth: 1,
    ...SHADOWS.sm,
  },
  goalPendingBg: {
    backgroundColor: '#FFFFFF',
    borderColor: '#EEEEEE',
  },
  goalCompletedBg: {
    backgroundColor: PALETTE.successMint,
    borderColor: PALETTE.successMintBorder,
  },
  goalLeftCol: {
    flexDirection: 'row',
    alignItems: 'center',
    flex: 1,
    marginRight: SPACING.md,
  },
  checkboxIndicator: {
    width: 22,
    height: 22,
    borderRadius: ROUNDED.sm,
    borderWidth: 2,
    borderColor: PALETTE.focusPink,
    marginRight: SPACING.md,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxCompleted: {
    backgroundColor: PALETTE.focusPink,
    borderColor: PALETTE.focusPink,
  },
  checkMarkIcon: {
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontSize: 14,
  },
  goalItemTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    flexShrink: 1,
  },
  goalMonthBadge: {
    fontSize: 10,
    fontWeight: 'bold',
    color: PALETTE.textGray,
    backgroundColor: 'rgba(0,0,0,0.05)',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: ROUNDED.md,
  },
  emptyContainer: {
    alignItems: 'center',
    padding: SPACING.xxl,
  },
});
