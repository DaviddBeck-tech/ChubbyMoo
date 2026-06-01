package com.example.model

import kotlin.random.Random

object MascotMessages {

    const val STATE_HAPPY = "HAPPY"
    const val STATE_REMIND = "REMIND"
    const val STATE_SAD_PONTED = "SAD_PONTED"
    const val STATE_ANGRY_ABANDONED = "ANGRY_ABANDONED"

    private val happyMessages = listOf(
        "Úi dồi ôi xuất sắc luôn! Cậu chăm chỉ thế này tớ tự hào xỉu up xỉu down! 🥛✨",
        "Hoàn thành nhiều task quá, bụng mỡ của tớ đang nhảy múa Samba đây này! 💃🐄",
        "Cứ thăng hoa thế này thì tụi mình xứng đáng đi du lịch đồng cỏ Thụy Sĩ quá cậu ơi! 🌴🌸",
        "Đỉnh chóp luôn! Nhìn cậu tích xanh mà tim tớ phơi phới như có nắng xuân dạt dào! 🍀🌷",
        "Mascot Bò Béo yêu cậu nhất trần đời! Hãy giữ vững phong độ siêu nhân này nha! 🥰🍼"
    )

    private val remindMessages = listOf(
        "Cậu ơi, đến giờ check-in Lovely Scheduler rồi! Không làm việc là tớ húp hết trà sữa của cậu đấy! 🍓🐄",
        "Hôm nay có mấy việc xinh xắn đang xếp hàng chờ cậu đó. Nhúc nhích tay chân làm thui nào! 🌸📝",
        "Đừng để công việc mọc rêu phong mốc meo nha, nhấc mông lên làm việc cùng tớ nhé! ☕🐮",
        "Một ngày mới nắng nhẹ rạng rỡ, làm tí việc cho đời thêm lấp lánh đi cậu iu ơi! 🌻✨",
        "Chỉ cần làm xong một việc nhỏ thôi, tụi mình sẽ gom đủ năng lượng cho cả ngày dài! 💪🐾"
    )

    private val sadPontedMessages = listOf(
        "Ơ kìa... Cậu lại dời lịch nữa rồi á? Tớ buồn thiu như cọng bún nhúng nước rồi đây... 🥺💔",
        "Huhu dời lịch thế này rồi ngày mai có làm thật không đó nha? Tớ lo lắm á! 🧐🐮",
        "Dời lịch lùi xuống là mập ra giống tớ đấy nha cậu ơi, đừng có trì hoãn nữa mà! 🥺🎒",
        "Nốt hôm nay thui nhé, ngày mai phải chiến đấu hết mình nha! Dời hoài tớ trầm cảm mất! 😿🌾",
        "Mỗi lần cậu dời lịch, mỡ bụng tớ lại tăng thêm một xíu vì buồn tủi... hix hix! 💔🐄"
    )

    private val angryAbandonedMessages = listOf(
        "QUÁ ĐÁNG LẮM NHE! GẦN 2 NGÀY RỒI CẬU KHÔNG NGÓ NGÀNG GÌ TỚ! CẬU CHÁN BÒ BÉO RỒI SAO?! 😤🔥",
        "Tớ đang đứng khoanh tay trước màn hình với gương mặt hờn dỗi chưa từng có đây! Làm việc ngay! 🤬🍓",
        "Cậu định bỏ rơi Lovely Scheduler và tớ thật sao? Đừng để tớ nổi điên đấy nhớ! 🤯💢",
        "Dỗi thật rồi! Mau vào tích xanh một task cho tớ nguôi bớt hỏa lò trong lòng đi xem nào! 😭💔",
        "Lười biếng thế này là không ngoan đâu nhé! Bò béo sắp phạt cậu bằng 100 câu mắng cute đấy! 😤🌾"
    )

    fun getNotificationContent(status: String): String {
        val list = when (status.uppercase()) {
            STATE_HAPPY -> happyMessages
            STATE_REMIND -> remindMessages
            STATE_SAD_PONTED, "SAD" -> sadPontedMessages
            STATE_ANGRY_ABANDONED, "ANGRY" -> angryAbandonedMessages
            else -> remindMessages
        }
        return list[Random.nextInt(list.size)]
    }
}
