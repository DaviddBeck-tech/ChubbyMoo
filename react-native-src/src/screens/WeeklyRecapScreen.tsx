import React, { useState, useMemo } from 'react';
import {
  StyleSheet,
  Text,
  View,
  ScrollView,
  TouchableOpacity,
  Image,
  Dimensions,
  Alert,
  Platform,
} from 'react-native';
import { PALETTE, SPACING, ROUNDED, SHADOWS, TYPOGRAPHY } from '../theme/theme';
import { useTaskStore, CATEGORIES } from '../store/taskStore';
import { Task } from '../types/types';
import { exportWeeklyReportPDF } from '../services/pdfExportService';

const { width } = Dimensions.get('window');
const cardSize = (width - SPACING.lg * 3) / 2; // Chiều rộng mỗi ô card lưới 2 cột

export default function WeeklyRecapScreen({ navigation }: any) {
  // Lấy các tác danh sách và hàm từ store
  const { tasks, getCurrentWeekTasks } = useTaskStore();

  // 1. Phân loại bộ lọc tab
  const [activeTab, setActiveTab] = useState<'all' | 'work' | 'personal'>('all');

  // Lọc danh sách công việc của tuần hiện thực tế
  const currentWeekTasks = useMemo(() => {
    return getCurrentWeekTasks ? getCurrentWeekTasks() : [];
  }, [tasks, getCurrentWeekTasks]);

  // Bộ lọc phụ thuộc vào tab được chọn
  const filteredTasks = useMemo(() => {
    return currentWeekTasks.filter((task) => {
      if (activeTab === 'all') return true;
      if (activeTab === 'work') return task.categoryId === 1; // Work category id
      // Nhóm cá nhân & học tập & sức khỏe (categoryId !== 1)
      return task.categoryId !== 1;
    });
  }, [currentWeekTasks, activeTab]);

  // Thống kê tiến độ tuần
  const stats = useMemo(() => {
    const total = currentWeekTasks.length;
    const completed = currentWeekTasks.filter((t) => t.status === 'completed').length;
    const percentage = total === 0 ? 0 : Math.round((completed / total) * 100);
    
    let evaluationMsg = 'Cố gắng lên nhé!';
    if (percentage === 100 && total > 0) evaluationMsg = 'Tuyệt vời tuyệt đối! 🎉';
    else if (percentage >= 80) evaluationMsg = 'Xuất sắc! 🌟';
    else if (percentage >= 50) evaluationMsg = 'Rất tiến bộ! 🌱';
    else if (total > 0) evaluationMsg = 'Cố lên một chút nữa nhé! 💪';

    return { total, completed, percentage, evaluationMsg };
  }, [currentWeekTasks]);

  // 2. State quản lý các tác vụ được tích chọn để xuất báo cáo
  // Trạng thái mặc định: tự động chọn tất cả các công việc trong danh sách lọc
  const [selectedTaskIds, setSelectedTaskIds] = useState<string[]>([]);

  // Tự động tích chọn tất cả các task có trong danh sách được lọc khi nhấn tab hoặc khởi tạo
  React.useEffect(() => {
    // Chỉ chọn các công việc hiện tại dựa theo filter để xuất báo cáo tối ưu nhất
    const ids = filteredTasks.map(t => t.id);
    setSelectedTaskIds(ids);
  }, [filteredTasks]);

  // Logic click chọn/bỏ chọn từng ô công việc
  const handleToggleSelectTask = (taskId: string) => {
    setSelectedTaskIds((prev) => {
      if (prev.includes(taskId)) {
        return prev.filter((id) => id !== taskId);
      } else {
        return [...prev, taskId];
      }
    });
  };

  // Click Chọn tất cả / Bỏ chọn tất cả trên màn hình
  const handleToggleSelectAll = () => {
    const currentFilteredIds = filteredTasks.map(t => t.id);
    const allSelectedInFilter = currentFilteredIds.every(id => selectedTaskIds.includes(id));

    if (allSelectedInFilter) {
      // Nếu đã chọn hết trong danh sách lọc, loại bỏ toàn bộ các ID thuộc danh sách lọc
      setSelectedTaskIds(prev => prev.filter(id => !currentFilteredIds.includes(id)));
    } else {
      // Ngược lại, thêm các ID chưa chọn vào danh sách
      setSelectedTaskIds(prev => {
        const uniqueIds = new Set([...prev, ...currentFilteredIds]);
        return Array.from(uniqueIds);
      });
    }
  };

  // 3. Thực hiện xuất báo cáo PDF chính thức
  const [isExporting, setIsExporting] = useState(false);

  const handleExportPDF = async () => {
    // Lọc danh sách các Task thực tế được chọn tương ứng với ID
    const selectedTasks = currentWeekTasks.filter(task => selectedTaskIds.includes(task.id));

    if (selectedTasks.length === 0) {
      Alert.alert(
        'Nhắc nhở nhẹ 💡',
        'Cậu hãy tích chọn ít nhất một tác vụ để xuất tệp báo cáo tuấn nhé!',
        [{ text: 'Đồng ý' }]
      );
      return;
    }

    try {
      setIsExporting(true);
      
      // Gọi service xuất PDF đã cấu hình
      await exportWeeklyReportPDF(selectedTasks);
      
      // Hiển thị toast đáng yêu sau khi xuất tệp hoàn thành thành công
      Alert.alert(
        'Xuất báo cáo thành công! 📈🚀',
        'Báo cáo tuần của cậu đã sẵn sàng để gửi đi rồi đó! Lên lịch xinh, báo cáo xịn nhé! ❤️',
        [{ text: 'Tuyệt vời!' }]
      );
    } catch (error: any) {
      Alert.alert('Có lỗi xảy ra 😢', error.message || 'Không thể tạo file báo cáo. Hãy thử lại sau cậu nhé!');
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView} contentContainerStyle={styles.scrollContent}>
        
        {/* ================= KHU VỰC 1: HEADER & STATS ================= */}
        <View style={styles.headerStatsArea}>
          <Text style={styles.screenHeading}>Nhìn Lại Tuần Qua ✨</Text>
          <Text style={styles.screenSubheading}>
            Cùng tổng hợp những cột mốc rạng rỡ và xuất báo cáo PDF chuẩn mực công sở!
          </Text>

          <View style={styles.progressCircleContainer}>
            {/* Vòng tròn phần trăm cute phong cách Pastel */}
            <View style={styles.progressRingOuter}>
              <View style={[styles.progressRingInner, { borderColor: stats.percentage > 0 ? PALETTE.primaryLavender : '#EEEEEE' }]}>
                <Text style={styles.progressPercentageText}>{stats.percentage}%</Text>
                <Text style={styles.progressStatusSub}>Đã Xong</Text>
              </View>
            </View>

            <View style={styles.progressTextColumn}>
              <Text style={styles.evalMsgText}>{stats.evaluationMsg}</Text>
              <Text style={styles.summaryStatsLabel}>
                Hoàn thành <Text style={styles.achievementHighlight}>{stats.completed}/{stats.total}</Text> tác vụ trong tuần.
              </Text>
              <View style={styles.progressBarBg}>
                <View style={[styles.progressBarFill, { width: `${stats.percentage}%` }]} />
              </View>
            </View>
          </View>
        </View>

        {/* ================= KHU VỰC 2: BỘ LỌC THÔNG MINH (FILTER TABS) ================= */}
        <View style={styles.filterSection}>
          <View style={styles.tabsRow}>
            <TouchableOpacity
              style={[styles.filterTab, activeTab === 'all' && styles.filterTabActive]}
              onPress={() => setActiveTab('all')}
            >
              <Text style={[styles.filterTabText, activeTab === 'all' && styles.filterTabTextActive]}>
                🌸 Tất cả ({currentWeekTasks.length})
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.filterTab, activeTab === 'work' && styles.filterTabActive]}
              onPress={() => setActiveTab('work')}
            >
              <Text style={[styles.filterTabText, activeTab === 'work' && styles.filterTabTextActive]}>
                💼 Công việc ({currentWeekTasks.filter(t => t.categoryId === 1).length})
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.filterTab, activeTab === 'personal' && styles.filterTabActive]}
              onPress={() => setActiveTab('personal')}
            >
              <Text style={[styles.filterTabText, activeTab === 'personal' && styles.filterTabTextActive]}>
                🌱 Cá nhân/Sức khỏe ({currentWeekTasks.filter(t => t.categoryId !== 1).length})
              </Text>
            </TouchableOpacity>
          </View>

          {/* Dải điều hướng con: Chọn nhanh tất cả hoặc Bỏ chọn */}
          <View style={styles.quickSelectorsRow}>
            <Text style={styles.itemsFilteredCount}>
              Đang hiển thị {filteredTasks.length} công việc
            </Text>
            {filteredTasks.length > 0 && (
              <TouchableOpacity style={styles.selectAllBtn} onPress={handleToggleSelectAll}>
                <Text style={styles.selectAllBtnText}>
                  {filteredTasks.every(id => selectedTaskIds.includes(id.id))
                    ? '☑️ Bỏ chọn hết'
                    : '✅ Chọn tất cả'}
                </Text>
              </TouchableOpacity>
            )}
          </View>
        </View>

        {/* ================= KHU VỰC 3: GRID DANH SÁCH CHECK-IN ================= */}
        {filteredTasks.length > 0 ? (
          <View style={styles.galleryGrid}>
            {filteredTasks.map((task) => {
              const category = CATEGORIES.find((c) => c.id === task.categoryId);
              const isSelected = selectedTaskIds.includes(task.id);
              const hasImage = task.status === 'completed' && task.proofImage;

              return (
                <TouchableOpacity
                  key={task.id}
                  activeOpacity={0.85}
                  style={[
                    styles.locketCard,
                    { borderColor: isSelected ? PALETTE.primaryLavender : 'transparent' },
                  ]}
                  onPress={() => handleToggleSelectTask(task.id)}
                >
                  {hasImage ? (
                    // LOCKET CARD: CÓ ẢNH THỰC TẾ
                    <View style={styles.cardImageWrapper}>
                      <Image source={{ uri: task.proofImage }} style={styles.cardBackgroundImage} />
                      
                      {/* Gradient đè mờ thủ công */}
                      <View style={styles.imageOverlayGradient}>
                        <Text style={styles.overlayTaskTitle} numberOfLines={1}>
                          {category?.emoji} {task.title}
                        </Text>
                        <Text style={styles.overlayCompletedTime}>
                          ⏱️ Done: {task.completedAt || 'Xong'}
                        </Text>
                      </View>
                    </View>
                  ) : (
                    // LOCKET CARD: KHÔNG CÓ ẢNH (PASTEL CONTAINER BACKGROUND)
                    <View
                      style={[
                        styles.cardPastelWrapper,
                        { backgroundColor: category?.color || '#FFFFFF' },
                      ]}
                    >
                      <View style={styles.emojiCircle}>
                        <Text style={styles.emojiText}>{category?.emoji || '🌸'}</Text>
                      </View>
                      
                      <View style={styles.noImageTextWrapper}>
                        <Text style={styles.pastelTaskTitle} numberOfLines={2}>
                          {task.title}
                        </Text>
                        <Text style={styles.pastelCategoryLabel}>
                          #{category?.nameVi || 'Khác'}
                        </Text>
                      </View>

                      {task.status === 'completed' && (
                        <View style={styles.pastelDoneFooter}>
                          <Text style={styles.pastelCompletedTime}>
                            ⏱️ Xong lúc: {task.completedAt || task.reminderTime}
                          </Text>
                        </View>
                      )}
                    </View>
                  )}

                  {/* Vòng tròn ôCheckbox nhỏ xinh nằm ở góc trên bên phải */}
                  <View style={[styles.checkboxIndicatorCircle, isSelected && styles.checkboxCircleSelected]}>
                    {isSelected && <Text style={styles.checkboxCheckmarkText}>✓</Text>}
                  </View>
                </TouchableOpacity>
              );
            })}
          </View>
        ) : (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyEmoji}>📦</Text>
            <Text style={styles.emptyTitle}>Danh sách tuần trống</Text>
            <Text style={styles.emptySubtitle}>
              Không tìm thấy công việc nào thỏa mãn bộ lọc hiện tại của cậu trong tuần này!
            </Text>
          </View>
        )}

      </ScrollView>

      {/* ================= NÚT XUẤT FILE BÁO CÁO PDF DƯỚI ĐÁY MÀN HÌNH ================= */}
      {currentWeekTasks.length > 0 && (
        <View style={styles.bottomBarButtonArea}>
          <TouchableOpacity
            style={[styles.exportPdfButton, isExporting && styles.exportBtnDisabled]}
            disabled={isExporting}
            onPress={handleExportPDF}
          >
            <Text style={styles.exportBtnText}>
              {isExporting ? '🔄 Đang khởi tạo PDF...' : '📈 Xuất Báo Cáo PDF Chuyên Nghiệp'}
            </Text>
          </TouchableOpacity>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: PALETTE.backgroundCream,
  },
  scrollView: {
    flex: 1,
  },
  scrollContent: {
    paddingBottom: 120, // Chừa khoảng trống bên dưới cho nút Submit
  },
  
  // KHU VỰC 1: HEADER & STATS
  headerStatsArea: {
    backgroundColor: '#FFFFFF',
    paddingHorizontal: SPACING.lg,
    paddingTop: Platform.OS === 'ios' ? 60 : 30,
    paddingBottom: SPACING.xl,
    borderBottomLeftRadius: ROUNDED.xxl,
    borderBottomRightRadius: ROUNDED.xxl,
    ...SHADOWS.md,
  },
  screenHeading: {
    ...TYPOGRAPHY.displayLarge,
    fontSize: 24,
    color: PALETTE.textCharcoal,
    marginBottom: SPACING.xs,
  },
  screenSubheading: {
    ...TYPOGRAPHY.bodyMedium,
    color: PALETTE.textGray,
    lineHeight: 18,
    marginBottom: SPACING.lg,
  },
  progressCircleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: PALETTE.backgroundCream,
    padding: SPACING.md,
    borderRadius: ROUNDED.lg,
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.03)',
  },
  progressRingOuter: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: '#FFFFFF',
    justifyContent: 'center',
    alignItems: 'center',
    ...SHADOWS.sm,
  },
  progressRingInner: {
    width: 68,
    height: 68,
    borderRadius: 34,
    backgroundColor: '#FFFFFF',
    borderWidth: 4,
    justifyContent: 'center',
    alignItems: 'center',
  },
  progressPercentageText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
  },
  progressStatusSub: {
    fontSize: 8,
    color: PALETTE.textLightGray,
    textTransform: 'uppercase',
    marginTop: -2,
    fontWeight: '700',
  },
  progressTextColumn: {
    flex: 1,
    marginLeft: SPACING.lg,
  },
  evalMsgText: {
    fontSize: 15,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginBottom: 4,
  },
  summaryStatsLabel: {
    ...TYPOGRAPHY.bodySmall,
    color: PALETTE.textGray,
    marginBottom: 8,
  },
  achievementHighlight: {
    fontWeight: 'bold',
    color: PALETTE.primaryLavender,
  },
  progressBarBg: {
    height: 8,
    backgroundColor: '#EAEAEA',
    borderRadius: 4,
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: PALETTE.primaryLavender,
    borderRadius: 4,
  },

  // KHU VỰC 2: FILTER TABS
  filterSection: {
    paddingHorizontal: SPACING.lg,
    marginTop: SPACING.lg,
  },
  tabsRow: {
    flexDirection: 'row',
    backgroundColor: '#FFFFFF',
    padding: 4,
    borderRadius: ROUNDED.md,
    justifyContent: 'space-between',
    ...SHADOWS.sm,
  },
  filterTab: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
    borderRadius: ROUNDED.md,
  },
  filterTabActive: {
    backgroundColor: PALETTE.secondaryPink,
  },
  filterTabText: {
    fontSize: 11,
    fontWeight: '600',
    color: PALETTE.textGray,
  },
  filterTabTextActive: {
    color: PALETTE.textCharcoal,
    fontWeight: '700',
  },
  quickSelectorsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: SPACING.md,
    paddingHorizontal: 4,
  },
  itemsFilteredCount: {
    fontSize: 12,
    fontWeight: '600',
    color: PALETTE.textLightGray,
  },
  selectAllBtn: {
    paddingVertical: 4,
    paddingHorizontal: 10,
    backgroundColor: 'rgba(255,255,255,0.7)',
    borderRadius: ROUNDED.sm,
    borderWidth: 1,
    borderColor: '#EAEAEA',
  },
  selectAllBtnText: {
    fontSize: 11,
    fontWeight: '700',
    color: PALETTE.textCharcoal,
  },

  // KHU VỰC 3: GRID LOCKER VIEWS
  galleryGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: SPACING.lg,
    justifyContent: 'space-between',
    marginTop: SPACING.sm,
  },
  locketCard: {
    width: cardSize,
    height: cardSize,
    borderRadius: 16,
    backgroundColor: '#FFFFFF',
    marginBottom: SPACING.lg,
    borderWidth: 2.5,
    overflow: 'hidden',
    ...SHADOWS.sm,
  },
  cardImageWrapper: {
    flex: 1,
    position: 'relative',
    justifyContent: 'flex-end',
  },
  cardBackgroundImage: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    width: '100%',
    height: '100%',
  },
  imageOverlayGradient: {
    padding: SPACING.md,
    backgroundColor: 'rgba(0, 0, 0, 0.45)',
    borderTopLeftRadius: 10,
    borderTopRightRadius: 10,
  },
  overlayTaskTitle: {
    fontSize: 12,
    fontWeight: 'bold',
    color: '#FFFFFF',
    marginBottom: 2,
  },
  overlayCompletedTime: {
    fontSize: 10,
    color: '#E0E0E0',
    fontWeight: '600',
  },
  cardPastelWrapper: {
    flex: 1,
    padding: SPACING.md,
    justifyContent: 'space-between',
  },
  emojiCircle: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255, 255, 255, 0.6)',
    justifyContent: 'center',
    alignItems: 'center',
    alignSelf: 'flex-start',
  },
  emojiText: {
    fontSize: 20,
  },
  noImageTextWrapper: {
    marginTop: 8,
    flex: 1,
    justifyContent: 'center',
  },
  pastelTaskTitle: {
    fontSize: 13,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    lineHeight: 16,
  },
  pastelCategoryLabel: {
    fontSize: 10,
    fontWeight: '700',
    color: 'rgba(0, 0, 0, 0.35)',
    marginTop: 2,
  },
  pastelDoneFooter: {
    borderTopWidth: 1,
    borderTopColor: 'rgba(0,0,0,0.05)',
    paddingTop: 6,
    marginTop: 4,
  },
  pastelCompletedTime: {
    fontSize: 10,
    fontWeight: 'bold',
    color: 'rgba(0,0,0,0.5)',
  },

  // CHECKBOX INDICATOR STYLES
  checkboxIndicatorCircle: {
    position: 'absolute',
    top: 8,
    right: 8,
    width: 22,
    height: 22,
    borderRadius: 11,
    borderWidth: 2,
    borderColor: '#FFFFFF',
    backgroundColor: 'rgba(0, 0, 0, 0.25)',
    justifyContent: 'center',
    alignItems: 'center',
    ...SHADOWS.sm,
  },
  checkboxCircleSelected: {
    backgroundColor: PALETTE.focusPink,
    borderColor: '#FFFFFF',
  },
  checkboxCheckmarkText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
    fontSize: 12,
    marginTop: -1,
  },

  // EMPTY PLACEHOLDER
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: SPACING.xxl,
    paddingVertical: 60,
  },
  emptyEmoji: {
    fontSize: 48,
    marginBottom: SPACING.md,
  },
  emptyTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: PALETTE.textCharcoal,
    marginBottom: SPACING.xs,
  },
  emptySubtitle: {
    fontSize: 12,
    color: PALETTE.textGray,
    textAlign: 'center',
    lineHeight: 18,
  },

  // BOTTOM SUBMIT BAR
  bottomBarButtonArea: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    paddingHorizontal: SPACING.lg,
    paddingVertical: SPACING.lg,
    backgroundColor: 'rgba(252, 250, 245, 0.95)',
    borderTopWidth: 1,
    borderColor: 'rgba(0,0,0,0.05)',
  },
  exportPdfButton: {
    backgroundColor: PALETTE.primaryLavender,
    paddingVertical: 15,
    borderRadius: ROUNDED.xl,
    alignItems: 'center',
    justifyContent: 'center',
    ...SHADOWS.md,
    borderWidth: 2.5,
    borderColor: '#FFFFFF',
  },
  exportBtnDisabled: {
    backgroundColor: PALETTE.textLightGray,
  },
  exportBtnText: {
    fontSize: 15,
    fontWeight: 'bold',
    color: '#FFFFFF',
    letterSpacing: 0.5,
  },
});
