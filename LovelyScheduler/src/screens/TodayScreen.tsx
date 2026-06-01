import React, { useState, useRef, useEffect } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  Animated,
  Dimensions,
  Modal,
  Platform,
  Image,
} from 'react-native';
import { PALETTE, SPACING, ROUNDED, SHADOWS, TYPOGRAPHY } from '../theme/theme';
import { useTaskStore, CATEGORIES } from '../store/taskStore';
import { Task } from '../types/types';
import { downloadProofImage } from '../services/downloadService';
import CustomCameraModal from './CustomCameraModal';

// Cute Individual animated task cards matching M3 style guidelines
function TaskCard({
  task,
  onToggle,
  onReschedule,
}: {
  task: Task;
  onToggle: () => void;
  onReschedule: (type: 'tomorrow' | 'next-week') => void;
}) {
  const category = CATEGORIES.find((c) => c.id === task.categoryId);
  const isCompleted = task.status === 'completed';
  const isMoved = task.status === 'moved';

  const bounceAnim = useRef(new Animated.Value(1)).current;
  const [showRescheduleMenu, setShowRescheduleMenu] = useState(false);

  const handleTogglePress = () => {
    Animated.sequence([
      Animated.timing(bounceAnim, {
        toValue: 0.94,
        duration: 80,
        useNativeDriver: true,
      }),
      Animated.spring(bounceAnim, {
        toValue: 1.05,
        friction: 4,
        tension: 140,
        useNativeDriver: true,
      }),
      Animated.spring(bounceAnim, {
        toValue: 1,
        friction: 5,
        useNativeDriver: true,
      }),
    ]).start(() => {
      onToggle();
    });
  };

  const cardColor = isCompleted
    ? PALETTE.successMint
    : isMoved
    ? PALETTE.warningPastel
    : category?.color || '#FFFFFF';

  const borderColor = isCompleted
    ? PALETTE.successMintBorder
    : isMoved
    ? PALETTE.warningPastelBorder
    : 'rgba(0,0,0,0.06)';

  if (isCompleted && task.proofImage) {
    return (
      <Animated.View
        style={[
          styles.cardContainer,
          {
            backgroundColor: cardColor,
            borderColor: borderColor,
            transform: [{ scale: bounceAnim }],
          },
        ]}
      >
        <View style={styles.cardHeader}>
          <View style={styles.categoryInfo}>
            <View style={styles.emojiCircle}>
              <Text style={styles.emojiText}>{category?.emoji || '🌸'}</Text>
            </View>
            <View>
              <Text style={styles.categoryNameVi}>{category?.nameVi || 'Công việc'}</Text>
            </View>
          </View>

          <View
            style={[
              styles.statusPill,
              {
                backgroundColor: 'rgba(255,255,255,0.4)',
              },
            ]}
          >
            <Text style={[styles.statusPillText, { color: PALETTE.textCharcoal }]}>ĐÃ XONG</Text>
          </View>
        </View>

        <View style={styles.cardBody}>
          <Text style={[styles.taskTitleText, styles.lineThroughText, { marginBottom: SPACING.xs }]}>
            {task.title}
          </Text>
          {task.description ? (
            <Text style={[styles.taskDescText, styles.completedMutedText, { marginBottom: SPACING.sm }]}>
              {task.description}
            </Text>
          ) : null}
        </View>

        {/* Locket Style Image */}
        <View style={[styles.proofImageWrapper, SHADOWS.sm]}>
          <Image source={{ uri: task.proofImage }} style={styles.proofImage} resizeMode="cover" />
          
          <View style={styles.timeTagOverlay}>
            <Text style={styles.timeTagText}>✨ Done lúc {task.completedAt || 'Xong'}</Text>
          </View>

          <TouchableOpacity
            style={styles.downloadBtn}
            onPress={() => downloadProofImage(task.proofImage!)}
            activeOpacity={0.7}
          >
            <Text style={styles.downloadBtnText}>📥</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.cardFooter}>
          <View style={styles.timeInfoRow}>
            <Text style={styles.clockIcon}>⏰</Text>
            <Text style={styles.timeText}>{task.reminderTime}</Text>
          </View>

          <TouchableOpacity
            style={styles.redoBtn}
            onPress={handleTogglePress}
            activeOpacity={0.8}
          >
            <Text style={styles.redoBtnText}>↺ Làm lại</Text>
          </TouchableOpacity>
        </View>
      </Animated.View>
    );
  }

  return (
    <Animated.View
      style={[
        styles.cardContainer,
        {
          backgroundColor: cardColor,
          borderColor: borderColor,
          transform: [{ scale: bounceAnim }],
        },
      ]}
    >
      <View style={styles.cardHeader}>
        <View style={styles.categoryInfo}>
          <View style={styles.emojiCircle}>
            <Text style={styles.emojiText}>{category?.emoji || '🌸'}</Text>
          </View>
          <View>
            <Text style={styles.categoryNameVi}>{category?.nameVi || 'Công việc'}</Text>
            {isMoved && task.originalDate && (
              <Text style={styles.originalDateLabel}>Dời từ {task.originalDate}</Text>
            )}
          </View>
        </View>

        <View
          style={[
            styles.statusPill,
            {
              backgroundColor: isCompleted
                ? 'rgba(255,255,255,0.4)'
                : isMoved
                ? PALETTE.warningPastelBorder
                : PALETTE.focusPink,
            },
          ]}
        >
          <Text
            style={[
              styles.statusPillText,
              {
                color: isMoved
                  ? PALETTE.warningPastelText
                  : isCompleted
                  ? PALETTE.textCharcoal
                  : '#FFFFFF',
              },
            ]}
          >
            {isCompleted ? 'XONG' : isMoved ? 'ĐÃ DỜI' : 'CHỜ'}
          </Text>
        </View>
      </View>

      <View style={styles.cardBody}>
        <Text style={[styles.taskTitleText, isCompleted && styles.lineThroughText]}>
          {task.title}
        </Text>
        <Text style={[styles.taskDescText, isCompleted && styles.completedMutedText]}>
          {task.description}
        </Text>
      </View>

      <View style={styles.cardFooter}>
        <View style={styles.timeInfoRow}>
          <Text style={styles.clockIcon}>⏰</Text>
          <Text style={styles.timeText}>{task.reminderTime}</Text>
        </View>

        <View style={styles.actionsBtnRow}>
          {!isCompleted && (
            <TouchableOpacity
              style={styles.actionBtnSecondary}
              onPress={() => setShowRescheduleMenu(true)}
            >
              <Text style={styles.actionBtnTextSec}>📅 Dời lịch</Text>
            </TouchableOpacity>
          )}

          <TouchableOpacity
            style={[
              styles.actionBtnPrimary,
              { backgroundColor: isCompleted ? PALETTE.successMintBorder : PALETTE.primaryLavender },
            ]}
            onPress={handleTogglePress}
          >
            <Text style={styles.actionBtnTextPri}>
              {isCompleted ? '✓ Đã xong' : '✓ Hoàn thành'}
            </Text>
          </TouchableOpacity>
        </View>
      </View>

      <Modal
        transparent
        visible={showRescheduleMenu}
        animationType="fade"
        onRequestClose={() => setShowRescheduleMenu(false)}
      >
        <TouchableOpacity
          style={styles.modalOverlay}
          activeOpacity={1}
          onPress={() => setShowRescheduleMenu(false)}
        >
          <View style={styles.popoverMenu}>
            <Text style={styles.popoverTitle}>🌟 Reschedule Task</Text>
            <Text style={styles.popoverDesc}>
              Dời "{task.title}" sang thời điểm khác:
            </Text>

            <TouchableOpacity
              style={styles.popoverOption}
              onPress={() => {
                onReschedule('tomorrow');
                setShowRescheduleMenu(false);
              }}
            >
              <Text style={styles.popoverOptionText}>➡️ Dời sang ngày mai (+1 Ngày)</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.popoverOption}
              onPress={() => {
                onReschedule('next-week');
                setShowRescheduleMenu(false);
              }}
            >
              <Text style={styles.popoverOptionText}>🗓️ Dời sang tuần sau (Thứ Hai tiếp theo)</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.popoverCancel}
              onPress={() => setShowRescheduleMenu(false)}
            >
              <Text style={styles.popoverCancelText}>Bỏ qua</Text>
            </TouchableOpacity>
          </View>
        </TouchableOpacity>
      </Modal>
    </Animated.View>
  );
}

export default function TodayScreen({ navigation }: any) {
  const { tasks, toggleTaskComplete, rescheduleTask } = useTaskStore();

  const [selectedDate, setSelectedDate] = useState<string>(new Date().toISOString().split('T')[0]);
  const [showBrainDumpModal, setShowBrainDumpModal] = useState<boolean>(false);

  const [cameraModalVisible, setCameraModalVisible] = useState(false);
  const [activeTaskForCamera, setActiveTaskForCamera] = useState<Task | null>(null);

  const handleTaskToggle = (task: Task) => {
    if (task.status !== 'completed') {
      setActiveTaskForCamera(task);
      setCameraModalVisible(true);
    } else {
      toggleTaskComplete(task.id);
    }
  };

  // Animated Scroll Offset & Bobbing Value
  const scrollY = useRef(new Animated.Value(0)).current;
  const bobbingAnim = useRef(new Animated.Value(0)).current;

  // Continuous subtle bobbing animation for the AI FAB
  useEffect(() => {
    Animated.loop(
      Animated.sequence([
        Animated.timing(bobbingAnim, {
          toValue: -4,
          duration: 1400,
          useNativeDriver: true,
        }),
        Animated.timing(bobbingAnim, {
          toValue: 4,
          duration: 1400,
          useNativeDriver: true,
        }),
      ])
    ).start();
  }, []);

  // Soft elastic sliding/shrinking calculated via scroll interpolation
  const fabWidth = scrollY.interpolate({
    inputRange: [0, 80],
    outputRange: [140, 56],
    extrapolate: 'clamp',
  });

  const fabTextOpacity = scrollY.interpolate({
    inputRange: [0, 50],
    outputRange: [1, 0],
    extrapolate: 'clamp',
  });

  // Derived Values
  const todayTasks = tasks.filter((task) => task.scheduledDate === selectedDate);
  const completedCount = todayTasks.filter((t) => t.status === 'completed').length;
  const progressPercent = todayTasks.length > 0 ? (completedCount / todayTasks.length) * 100 : 0;

  // Week days starting from Monday of selected week
  const getWeekDates = (dateStr: string) => {
    const current = new Date(dateStr);
    const day = current.getDay();
    const diff = current.getDate() - day + (day === 0 ? -6 : 1); // Monday
    const monday = new Date(current.setDate(diff));

    const days = [];
    for (let i = 0; i < 7; i++) {
      const nextDate = new Date(monday);
      nextDate.setDate(monday.getDate() + i);
      days.push(nextDate);
    }
    return days;
  };

  const weekDays = getWeekDates(selectedDate);

  return (
    <View style={styles.container}>
      {/* 🌸 HEADER SECTION WITH DYNAMIC WEEK VIEW & STATS */}
      <View style={styles.header}>
        <View style={styles.headerTop}>
          <View>
            <Text style={styles.screenTitle}>Lovely Scheduler 🌸</Text>
            <Text style={styles.todayDateText}>Lên lịch xinh, làm việc xịn</Text>
          </View>
          <TouchableOpacity
            style={styles.avatarFrame}
            onPress={() => setSelectedDate(new Date().toISOString().split('T')[0])}
          >
            <View style={styles.avatarInner} />
          </TouchableOpacity>
        </View>

        {/* --- Dynamic Week Selector --- */}
        <View style={styles.weekSelectorContainer}>
          <Text style={styles.monthLabel}>Tháng {new Date(selectedDate).getMonth() + 1}</Text>
          <View style={styles.weekDaysRow}>
            {weekDays.map((day, idx) => {
              const dateStr = day.toISOString().split('T')[0];
              const isSelected = dateStr === selectedDate;
              const dayNames = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];
              return (
                <TouchableOpacity
                  key={idx}
                  style={[
                    styles.weekDayButton,
                    isSelected && styles.weekDayButtonSelected,
                  ]}
                  onPress={() => setSelectedDate(dateStr)}
                >
                  <Text style={[styles.weekDayName, isSelected && styles.weekDaySelectedText]}>
                    {dayNames[idx]}
                  </Text>
                  <Text style={[styles.weekDayNum, isSelected && styles.weekDaySelectedText]}>
                    {day.getDate()}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>
      </View>

      {/* 📜 SINGLE UNIFIED VERTICAL SCROLL VIEW */}
      <ScrollView
        style={styles.scroller}
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
        overScrollMode="never"
        bounces={true}
        onScroll={Animated.event(
          [{ nativeEvent: { contentOffset: { y: scrollY } } }],
          { useNativeDriver: false }
        )}
        scrollEventThrottle={16}
      >
        {/* 1. PROGRESS ESTIMATION CARD */}
        <TouchableOpacity
          style={styles.progressCard}
          onPress={() => navigation.navigate('WeeklyRecap')}
          activeOpacity={0.9}
        >
          <View style={styles.progressCardHeader}>
            <Text style={styles.progressCardTitle}>Tiến Độ Lên Lịch Tuần 📈</Text>
            <Text style={styles.progressBadge}>🍉 Bò Sữa Béo</Text>
          </View>
          <View style={styles.progressBarWrapper}>
            <View style={[styles.progressBarInner, { width: `${Math.max(5, progressPercent)}%` }]} />
          </View>
          <Text style={styles.progressCardDesc}>
            Cậu đã hoàn thành <Text style={styles.boldText}>{completedCount}/{todayTasks.length}</Text> công việc hôm nay ({Math.round(progressPercent)}%). Nhấp vào để xem chi tiết tuần qua! ✨
          </Text>
        </TouchableOpacity>

        {/* 2. DYNAMIC DUOLINGO MASCOT EMOTION CARD */}
        <View style={styles.mascotCard}>
          <View style={styles.mascotInfo}>
            <Text style={styles.mascotEmoji}>🐮🥛</Text>
            <View style={{ flex: 1, marginLeft: 10 }}>
              <Text style={styles.mascotStateTitle}>Vườn cảm xúc Bò Béo</Text>
              <Text style={styles.mascotStateText}>
                {progressPercent >= 100
                  ? "Bò Béo cực kỳ múp míp, nhảy nhót tung tăng vì cậu rùa vàng đã xong hết việc! 🌿🎉"
                  : progressPercent >= 50
                  ? "Bò Béo đang rất hạnh phúc ngậm sữa dâu rùi cậu ơi! Tiếp tục hoàn thành việc nha! 🥛🌸"
                  : "Moo... Bò Béo đang đói sữaaaa, hix cậu làm việc để Bò Béo vui lên nha! 🥺🍼"}
              </Text>
            </View>
          </View>
        </View>

        {/* 3. TASK LIST HEADER */}
        <Text style={styles.taskListTitle}>List công việc ngày {new Date(selectedDate).getDate()} 📝</Text>

        {/* 4. TASK ITEMS LIST (DYNAMIC MAPPED - NO NESTED SCROLLVIEW) */}
        {todayTasks.length > 0 ? (
          todayTasks.map((task) => (
            <TaskCard
              key={task.id}
              task={task}
              onToggle={() => handleTaskToggle(task)}
              onReschedule={(type) => rescheduleTask(task.id, type)}
            />
          ))
        ) : (
          <View style={styles.emptyTasksContainer}>
            <Text style={styles.emptyEmoji}>🍃</Text>
            <Text style={styles.emptyTitle}>Lịch trống xinh xắn!</Text>
            <Text style={styles.emptyDesc}>
              Không có nhiệm vụ nào cả. Hãy bấm nút Xả Não AI ở góc phải để thảo luận lập kế hoạch, hoặc lướt chuẩn bị thêm việc mới nhé! ✨
            </Text>
          </View>
        )}
      </ScrollView>

      {/* 🚀 FLOAT AND SHRINKING AI BRAIN-DUMP FAB */}
      <Animated.View
        style={[
          styles.aiFabContainer,
          {
            width: fabWidth,
            transform: [{ translateY: bobbingAnim }],
          },
        ]}
      >
        <TouchableOpacity
          style={styles.aiFabInner}
          activeOpacity={0.8}
          onPress={() => navigation.navigate('SundayRitual')}
        >
          <Text style={styles.aiFabEmoji}>🧠</Text>
          <Animated.Text
            style={[
              styles.aiFabText,
              {
                opacity: fabTextOpacity,
              },
            ]}
            numberOfLines={1}
          >
            Xả não AI
          </Animated.Text>
          <View style={styles.aiBadge}>
            <Text style={styles.aiBadgeText}>AI ✨</Text>
          </View>
        </TouchableOpacity>
      </Animated.View>

      {/* ➕ STANDARD ADD TASK FAB */}
      <TouchableOpacity
        style={styles.addFAB}
        activeOpacity={0.8}
        onPress={() => navigation.navigate('CreateTaskModal', { selectedDateString: selectedDate })}
      >
        <Text style={styles.addFABText}>+</Text>
      </TouchableOpacity>

      {activeTaskForCamera && (
        <CustomCameraModal
          visible={cameraModalVisible}
          taskTitle={activeTaskForCamera.title}
          onClose={() => {
            setCameraModalVisible(false);
            setActiveTaskForCamera(null);
          }}
          onCapture={(imageUri) => {
            const now = new Date();
            const hours = String(now.getHours()).padStart(2, '0');
            const minutes = String(now.getMinutes()).padStart(2, '0');
            const formattedTime = `${hours}:${minutes}`;
            toggleTaskComplete(activeTaskForCamera.id, imageUri, formattedTime);
            setCameraModalVisible(false);
            setActiveTaskForCamera(null);
          }}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: PALETTE.backgroundCream,
  },
  header: {
    backgroundColor: '#FFFFFF',
    paddingTop: Platform.OS === 'ios' ? 54 : 32,
    paddingBottom: SPACING.lg,
    paddingHorizontal: SPACING.xl,
    borderBottomLeftRadius: ROUNDED.xxl,
    borderBottomRightRadius: ROUNDED.xxl,
    ...SHADOWS.md,
  },
  headerTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.md,
  },
  screenTitle: {
    ...TYPOGRAPHY.titleMedium,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  todayDateText: {
    fontSize: 12,
    color: PALETTE.textGray,
    marginTop: 2,
  },
  avatarFrame: {
    width: 40,
    height: 40,
    borderRadius: ROUNDED.xl,
    backgroundColor: PALETTE.secondaryPink,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: '#FFFFFF',
    ...SHADOWS.sm,
  },
  avatarInner: {
    width: 18,
    height: 18,
    borderRadius: 9,
    backgroundColor: PALETTE.focusPink,
  },
  weekSelectorContainer: {
    marginTop: SPACING.xs,
  },
  monthLabel: {
    fontSize: 13,
    fontWeight: 'bold',
    color: PALETTE.primaryLavender,
    marginBottom: 8,
  },
  weekDaysRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  weekDayButton: {
    width: 40,
    height: 52,
    borderRadius: ROUNDED.md,
    backgroundColor: '#FAFAFA',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#EEEEEE',
  },
  weekDayButtonSelected: {
    backgroundColor: PALETTE.primaryLavender,
    borderColor: PALETTE.primaryLavender,
    ...SHADOWS.sm,
  },
  weekDayName: {
    fontSize: 9,
    color: PALETTE.textGray,
    fontWeight: '600',
    marginBottom: 2,
  },
  weekDayNum: {
    fontSize: 14,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  weekDaySelectedText: {
    color: '#FFFFFF',
  },
  scroller: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: SPACING.xl,
    paddingTop: SPACING.xl,
    paddingBottom: 150, // Extra padding as bottom block spacer
  },
  progressCard: {
    backgroundColor: '#FFFFFF',
    borderRadius: ROUNDED.xxl,
    padding: SPACING.lg,
    marginBottom: SPACING.lg,
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.03)',
    ...SHADOWS.sm,
  },
  progressCardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
  },
  progressCardTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  progressBadge: {
    fontSize: 10,
    fontWeight: 'bold',
    color: PALETTE.warningPastelText,
    backgroundColor: PALETTE.backgroundCream,
    paddingHorizontal: 6,
    paddingVertical: 3,
    borderRadius: 6,
  },
  progressBarWrapper: {
    height: 10,
    backgroundColor: '#F3F3F3',
    borderRadius: ROUNDED.full,
    overflow: 'hidden',
    marginBottom: 8,
  },
  progressBarInner: {
    height: '100%',
    backgroundColor: PALETTE.primaryLavender,
    borderRadius: ROUNDED.full,
  },
  progressCardDesc: {
    fontSize: 11,
    color: PALETTE.textGray,
  },
  boldText: {
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  mascotCard: {
    backgroundColor: '#FFF0F5',
    borderRadius: ROUNDED.xxl,
    padding: SPACING.lg,
    marginBottom: SPACING.lg,
    borderWidth: 1,
    borderColor: 'rgba(255,105,180,0.1)',
    ...SHADOWS.sm,
  },
  mascotInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  mascotEmoji: {
    fontSize: 32,
  },
  mascotStateTitle: {
    fontSize: 13,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginBottom: 2,
  },
  mascotStateText: {
    fontSize: 11,
    color: PALETTE.textGray,
    lineHeight: 16,
  },
  taskListTitle: {
    ...TYPOGRAPHY.bodyMedium,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginTop: SPACING.sm,
    marginBottom: SPACING.md,
  },
  cardContainer: {
    borderRadius: ROUNDED.xxl,
    padding: SPACING.lg,
    marginBottom: SPACING.lg,
    borderWidth: 1,
    ...SHADOWS.sm,
  },
  cardHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SPACING.sm,
  },
  categoryInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  emojiCircle: {
    width: 32,
    height: 32,
    borderRadius: ROUNDED.full,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: SPACING.sm,
    ...SHADOWS.sm,
  },
  emojiText: {
    fontSize: 16,
  },
  categoryNameVi: {
    fontSize: 11,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  originalDateLabel: {
    fontSize: 8,
    color: PALETTE.warningPastelText,
    fontWeight: '500',
  },
  statusPill: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: ROUNDED.md,
  },
  statusPillText: {
    fontSize: 8,
    fontWeight: 'bold',
  },
  cardBody: {
    marginBottom: SPACING.md,
  },
  taskTitleText: {
    fontSize: 15,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginBottom: 4,
  },
  taskDescText: {
    fontSize: 12,
    color: PALETTE.textGray,
    lineHeight: 17,
  },
  lineThroughText: {
    textDecorationLine: 'line-through',
    color: PALETTE.textLightGray,
  },
  completedMutedText: {
    opacity: 0.5,
  },
  cardFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    borderTopWidth: 1,
    borderTopColor: 'rgba(0,0,0,0.04)',
    paddingTop: SPACING.md,
  },
  timeInfoRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  clockIcon: {
    marginRight: 4,
    fontSize: 12,
  },
  timeText: {
    fontSize: 11,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  actionsBtnRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  actionBtnSecondary: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#EEEEEE',
    borderRadius: ROUNDED.lg,
    paddingVertical: 5,
    paddingHorizontal: 10,
    marginRight: SPACING.xs,
    ...SHADOWS.sm,
  },
  actionBtnTextSec: {
    fontSize: 11,
    fontWeight: 'bold',
    color: PALETTE.textGray,
  },
  actionBtnPrimary: {
    borderRadius: ROUNDED.lg,
    paddingVertical: 5,
    paddingHorizontal: 10,
    ...SHADOWS.sm,
  },
  actionBtnTextPri: {
    fontSize: 11,
    fontWeight: 'bold',
    color: '#FFFFFF',
  },
  emptyTasksContainer: {
    alignItems: 'center',
    paddingVertical: 40,
    paddingHorizontal: SPACING.xl,
  },
  emptyEmoji: {
    fontSize: 40,
    marginBottom: SPACING.sm,
  },
  emptyTitle: {
    fontSize: 14,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginBottom: 4,
  },
  emptyDesc: {
    fontSize: 11,
    color: PALETTE.textGray,
    textAlign: 'center',
    lineHeight: 17,
  },
  aiFabContainer: {
    position: 'absolute',
    bottom: 96,
    right: 24,
    height: 56,
    borderRadius: 28,
    backgroundColor: '#E0BBE4',
    borderWidth: 3,
    borderColor: '#FFFFFF',
    ...SHADOWS.lg,
    overflow: 'visible',
  },
  aiFabInner: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%',
    paddingHorizontal: 16,
  },
  aiFabEmoji: {
    fontSize: 22,
  },
  aiFabText: {
    fontSize: 13,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginLeft: 6,
  },
  aiBadge: {
    position: 'absolute',
    top: -8,
    right: -4,
    backgroundColor: PALETTE.focusPink,
    paddingHorizontal: 4,
    paddingSpacer: 2,
    paddingVertical: 1,
    borderRadius: 6,
  },
  aiBadgeText: {
    fontSize: 7,
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  addFAB: {
    position: 'absolute',
    bottom: 24,
    right: 24,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: PALETTE.primaryLavender,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 3,
    borderColor: '#FFFFFF',
    ...SHADOWS.lg,
  },
  addFABText: {
    fontSize: 28,
    color: '#FFFFFF',
    fontWeight: '300',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.4)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  popoverMenu: {
    width: Dimensions.get('window').width * 0.8,
    backgroundColor: '#FFFFFF',
    borderRadius: ROUNDED.xxl,
    padding: SPACING.xl,
    ...SHADOWS.lg,
  },
  popoverTitle: {
    fontSize: 15,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginBottom: 4,
    textAlign: 'center',
  },
  popoverDesc: {
    fontSize: 12,
    color: PALETTE.textGray,
    marginBottom: SPACING.lg,
    textAlign: 'center',
  },
  popoverOption: {
    backgroundColor: PALETTE.backgroundCream,
    padding: SPACING.md,
    borderRadius: ROUNDED.xl,
    marginBottom: SPACING.sm,
    borderWidth: 1,
    borderColor: '#EEEEEE',
  },
  popoverOptionText: {
    fontWeight: 'bold',
    fontSize: 12,
    color: PALETTE.textCharcoal,
  },
  popoverCancel: {
    paddingVertical: SPACING.sm,
    alignItems: 'center',
    marginTop: SPACING.xs,
  },
  popoverCancelText: {
    fontWeight: 'bold',
    color: PALETTE.textGray,
  },
  proofImageWrapper: {
    width: '100%',
    aspectRatio: 1,
    borderRadius: ROUNDED.lg,
    overflow: 'hidden',
    position: 'relative',
    marginBottom: SPACING.md,
    borderWidth: 2,
    borderColor: 'rgba(255,255,255,0.6)',
  },
  proofImage: {
    width: '100%',
    height: '100%',
  },
  timeTagOverlay: {
    position: 'absolute',
    top: SPACING.sm,
    left: SPACING.sm,
    backgroundColor: 'rgba(0,0,0,0.55)',
    paddingHorizontal: SPACING.sm,
    paddingVertical: SPACING.xs,
    borderRadius: ROUNDED.sm,
  },
  timeTagText: {
    color: '#FFFFFF',
    fontSize: 10,
    fontWeight: 'bold',
  },
  downloadBtn: {
    position: 'absolute',
    bottom: SPACING.sm,
    right: SPACING.sm,
    backgroundColor: '#FFFFFF',
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: PALETTE.secondaryPink,
    ...SHADOWS.sm,
  },
  downloadBtnText: {
    fontSize: 18,
  },
  redoBtn: {
    backgroundColor: 'rgba(255,255,255,0.5)',
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.06)',
    borderRadius: ROUNDED.lg,
    paddingVertical: 5,
    paddingHorizontal: 10,
    ...SHADOWS.sm,
  },
  redoBtnText: {
    fontSize: 11,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
});
