import React, { useState, useEffect, useRef } from 'react';
import {
  StyleSheet,
  Text,
  View,
  Modal,
  TextInput,
  TouchableOpacity,
  Animated,
  Dimensions,
  TouchableWithoutFeedback,
  Keyboard,
  Platform,
} from 'react-native';
import { PALETTE, SPACING, ROUNDED, SHADOWS, TYPOGRAPHY } from '../theme/theme';
import { useTaskStore, CATEGORIES } from '../store/taskStore';

interface CreateTaskModalProps {
  isVisible: boolean;
  onClose: () => void;
  selectedDateString: string; // Dynamic date to assign this task to (e.g. '2026-05-28')
}

export default function CreateTaskModal({ isVisible, onClose, selectedDateString }: CreateTaskModalProps) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [selectedCategoryId, setSelectedCategoryId] = useState(1);
  const [reminderTime, setReminderTime] = useState('09:00');

  // Zustand dynamic state actions
  const addTask = useTaskStore((state) => state.addTask);

  // Animated setup for Bottom Sheet sliding & backdrop fade
  const slideAnim = useRef(new Animated.Value(Dimensions.get('window').height)).current;
  const fadeAnim = useRef(new Animated.Value(0)).current;

  // Track each category scale animation for adorable bouncy micro-interactions when selected
  const scaleAnims = useRef<Record<number, Animated.Value>>({
    1: new Animated.Value(1),
    2: new Animated.Value(1),
    3: new Animated.Value(1),
    4: new Animated.Value(1),
  }).current;

  useEffect(() => {
    if (isVisible) {
      // Trigger sequence on modal opening
      Animated.parallel([
        Animated.timing(fadeAnim, {
          toValue: 0.5,
          duration: 300,
          useNativeDriver: true,
        }),
        Animated.spring(slideAnim, {
          toValue: 0,
          friction: 8,
          tension: 40,
          useNativeDriver: true,
        }),
      ]).start();
    } else {
      // Clean up inputs on hide
      setTitle('');
      setDescription('');
      setSelectedCategoryId(1);
      setReminderTime('09:00');
    }
  }, [isVisible]);

  // Spring animations for category buttons
  const triggerCategoryBounce = (catId: number) => {
    // Reset other scale animations to original scale (1)
    Object.keys(scaleAnims).forEach((key) => {
      const id = parseInt(key, 10);
      if (id !== catId) {
        Animated.spring(scaleAnims[id], {
          toValue: 1,
          friction: 6,
          useNativeDriver: true,
        }).start();
      }
    });

    // Bounce-into active zoom for chosen pastel category
    Animated.sequence([
      Animated.spring(scaleAnims[catId], {
        toValue: 1.2,
        friction: 4,
        tension: 100,
        useNativeDriver: true,
      }),
      Animated.spring(scaleAnims[catId], {
        toValue: 1.1,
        friction: 6,
        useNativeDriver: true,
      }),
    ]).start();
  };

  const handleCategorySelect = (categoryId: number) => {
    setSelectedCategoryId(categoryId);
    triggerCategoryBounce(categoryId);
  };

  // Close with custom reverse slide-down animation
  const handleAnimateClose = () => {
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 0,
        duration: 250,
        useNativeDriver: true,
      }),
      Animated.timing(slideAnim, {
        toValue: Dimensions.get('window').height,
        duration: 250,
        useNativeDriver: true,
      }),
    ]).start(() => {
      onClose();
    });
  };

  // Action: Save and propagate task into Zustand Store
  const handleSaveTask = () => {
    if (!title.trim()) {
      alert('Vui lòng nhập tên công việc nha! 🥰');
      return;
    }

    /* 
      ========================================================
      💡 CƠ CHẾ TRUYỀN DỮ LIỆU VÀO ZUSTAND STORE:
      --------------------------------------------------------
      Khi người dùng nhấn button "Hoàn Tất Lên Lịch", 
      hàm 'addTask()' dưới đây được kích hoạt.
      Hàm này gọi trực tiếp action `addTask` đã định nghĩa 
      trong useTaskStore.
      Zustand lúc này sẽ:
        1. Nhận các tham số được nhập từ UI (tên, mô tả, danh mục, ngày, giờ).
        2. Tạo ra object Task với ID duy nhất và tính toán tuần hiện tại của năm.
        3. Cập nhật state list `tasks` mới và trigger cập nhật cho toàn bộ 
           ứng dụng, đồng thời kích hoạt AsyncStorage lưu lại offline an toàn.
      ========================================================
    */
    addTask(
      title.trim(),
      description.trim() || 'Hạnh phúc là hoàn thành mục tiêu nhỏ mỗi ngày!',
      selectedCategoryId,
      selectedDateString,
      reminderTime
    );

    // Close the cozy modal with custom sliding transition
    handleAnimateClose();
  };

  return (
    <Modal
      transparent
      visible={isVisible}
      animationType="none"
      onRequestClose={handleAnimateClose}
    >
      <TouchableWithoutFeedback onPress={Keyboard.dismiss}>
        <View style={styles.modalOverlay}>
          {/* Fading backdrop frame */}
          <TouchableWithoutFeedback onPress={handleAnimateClose}>
            <Animated.View style={[styles.backdrop, { opacity: fadeAnim }]} />
          </TouchableWithoutFeedback>

          {/* Sliding Bottom Sheet container with custom border radiuses */}
          <Animated.View
            style={[
              styles.bottomSheet,
              { transform: [{ translateY: slideAnim }] },
            ]}
          >
            {/* Header pull accent bar */}
            <View style={styles.pullContainer}>
              <View style={styles.pullBar} />
            </View>

            <Text style={styles.modalTitle}>✨ Tạo Hoạt Động Xinh</Text>
            <Text style={styles.modalSubtitle}>Được lên lịch cho {selectedDateString}</Text>

            {/* Input 1: Task Title */}
            <Text style={styles.sectionHeading}>Tên công việc</Text>
            <TextInput
              style={styles.textInputStyle}
              placeholder="Ví dụ: Thiền buổi sáng, Tưới cây..."
              placeholderTextColor={PALETTE.textLightGray}
              value={title}
              onChangeText={setTitle}
            />

            {/* Input 2: Task Description */}
            <Text style={styles.sectionHeading}>Ghi chú chi tiết</Text>
            <TextInput
              style={[styles.textInputStyle, styles.textAreaStyle]}
              placeholder="Ghi lại một chút lời nhắn nhủ yêu thương cho chính mình..."
              placeholderTextColor={PALETTE.textLightGray}
              multiline
              numberOfLines={3}
              value={description}
              onChangeText={setDescription}
            />

            {/* Input 3: Cute Category Selector Grid */}
            <Text style={styles.sectionHeading}>Chọn Danh mục (Categories)</Text>
            <View style={styles.categoriesRow}>
              {CATEGORIES.map((category) => {
                const isSelected = selectedCategoryId === category.id;
                const animatedScale = scaleAnims[category.id] || new Animated.Value(1);

                return (
                  <TouchableOpacity
                    key={category.id}
                    activeOpacity={0.8}
                    onPress={() => handleCategorySelect(category.id)}
                    style={styles.categoryTouchArea}
                  >
                    <Animated.View
                      style={[
                        styles.categoryCircledBadge,
                        {
                          backgroundColor: category.color,
                          borderColor: isSelected ? PALETTE.primaryLavender : '#FFFFFF',
                          borderWidth: isSelected ? 3 : 1,
                          transform: [{ scale: animatedScale }],
                        },
                      ]}
                    >
                      <Text style={styles.categoryEmoji}>{category.emoji}</Text>
                    </Animated.View>
                    <Text
                      style={[
                        styles.categoryLabelText,
                        isSelected && { color: PALETTE.textCharcoal, fontWeight: 'bold' },
                      ]}
                    >
                      {category.nameVi}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>

            {/* Input 4: Quick Time Picker Row */}
            <View style={styles.timePickerContainer}>
              <View style={styles.timeInfoCol}>
                <Text style={styles.timeLabel}>⏰ Giờ nhắc nhở</Text>
                <Text style={styles.timeValueText}>{reminderTime}</Text>
              </View>

              {/* Quick Preset Hours for easy beautiful mockup selection */}
              <View style={styles.presetsRow}>
                {['07:30', '09:00', '15:30', '21:00'].map((timePreset) => {
                  const isActive = reminderTime === timePreset;
                  return (
                    <TouchableOpacity
                      key={timePreset}
                      style={[
                        styles.presetTimeBtn,
                        isActive && styles.presetTimeBtnActive,
                      ]}
                      onPress={() => setReminderTime(timePreset)}
                    >
                      <Text
                        style={[
                          styles.presetTimeText,
                          isActive && styles.presetTimeTextActive,
                        ]}
                      >
                        {timePreset}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>

            {/* 💾 ACTIONS ROW */}
            <View style={styles.actionRow}>
              <TouchableOpacity
                style={styles.cancelBtn}
                onPress={handleAnimateClose}
              >
                <Text style={styles.cancelBtnText}>Bỏ qua</Text>
              </TouchableOpacity>

              <TouchableOpacity
                style={styles.submitBtn}
                onPress={handleSaveTask}
              >
                <Text style={styles.submitBtnText}>Hoàn Tất Lên Lịch 🌸</Text>
              </TouchableOpacity>
            </View>
          </Animated.View>
        </View>
      </TouchableWithoutFeedback>
    </Modal>
  );
}

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: '#333333',
  },
  bottomSheet: {
    backgroundColor: PALETTE.backgroundCream,
    borderTopLeftRadius: ROUNDED.xxl,
    borderTopRightRadius: ROUNDED.xxl,
    paddingHorizontal: SPACING.xl,
    paddingBottom: Platform.OS === 'ios' ? 40 : SPACING.xxl,
    ...SHADOWS.lg,
  },
  pullContainer: {
    width: '100%',
    alignItems: 'center',
    paddingVertical: SPACING.md,
  },
  pullBar: {
    width: 40,
    height: 5,
    borderRadius: 3,
    backgroundColor: '#CCCCCC',
  },
  modalTitle: {
    ...TYPOGRAPHY.titleLarge,
    textAlign: 'center',
    marginBottom: 4,
  },
  modalSubtitle: {
    ...TYPOGRAPHY.bodySmall,
    color: PALETTE.textGray,
    textAlign: 'center',
    marginBottom: SPACING.lg,
  },
  sectionHeading: {
    ...TYPOGRAPHY.labelBold,
    color: PALETTE.textCharcoal,
    marginTop: SPACING.md,
    marginBottom: SPACING.sm,
  },
  textInputStyle: {
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#EEEEEE',
    borderRadius: ROUNDED.lg,
    padding: SPACING.md,
    fontSize: 14,
    color: PALETTE.textCharcoal,
    ...SHADOWS.sm,
  },
  textAreaStyle: {
    height: 70,
    textAlignVertical: 'top',
  },
  categoriesRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginVertical: SPACING.xs,
  },
  categoryTouchArea: {
    alignItems: 'center',
    flex: 1,
  },
  categoryCircledBadge: {
    width: 52,
    height: 52,
    borderRadius: ROUNDED.full,
    justifyContent: 'center',
    alignItems: 'center',
    ...SHADOWS.sm,
    marginBottom: SPACING.xs,
  },
  categoryEmoji: {
    fontSize: 22,
  },
  categoryLabelText: {
    fontSize: 11,
    color: PALETTE.textGray,
    fontWeight: '500',
  },
  timePickerContainer: {
    backgroundColor: '#FFFFFF',
    borderRadius: ROUNDED.xl,
    padding: SPACING.md,
    marginVertical: SPACING.md,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: '#EEEEEE',
    ...SHADOWS.sm,
  },
  timeInfoCol: {
    flex: 1,
  },
  timeLabel: {
    fontSize: 12,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  timeValueText: {
    fontSize: 18,
    fontWeight: '800',
    color: PALETTE.primaryLavender,
    marginTop: 2,
  },
  presetsRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  presetTimeBtn: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: ROUNDED.md,
    backgroundColor: PALETTE.backgroundCream,
    marginLeft: 6,
    borderWidth: 1,
    borderColor: '#EEEEEE',
  },
  presetTimeBtnActive: {
    backgroundColor: PALETTE.secondaryPink,
    borderColor: PALETTE.focusPink,
  },
  presetTimeText: {
    fontSize: 11,
    fontWeight: '600',
    color: PALETTE.textGray,
  },
  presetTimeTextActive: {
    color: PALETTE.textCharcoal,
    fontWeight: 'bold',
  },
  actionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: SPACING.lg,
  },
  cancelBtn: {
    paddingVertical: SPACING.md,
    paddingHorizontal: SPACING.xl,
    borderRadius: ROUNDED.xl,
    backgroundColor: '#FFFFFF',
    borderWidth: 1,
    borderColor: '#EEEEEE',
    marginRight: SPACING.sm,
  },
  cancelBtnText: {
    color: PALETTE.textGray,
    fontWeight: 'bold',
  },
  submitBtn: {
    flex: 1,
    paddingVertical: SPACING.md,
    borderRadius: ROUNDED.xl,
    backgroundColor: PALETTE.primaryLavender,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#D1A6D7',
    ...SHADOWS.md,
  },
  submitBtnText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: 'bold',
  },
});
