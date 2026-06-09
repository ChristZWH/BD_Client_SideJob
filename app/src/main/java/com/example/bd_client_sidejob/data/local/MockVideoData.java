package com.example.bd_client_sidejob.data.local;

import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 视频假数据提供者
 *
 * 设计原则：
 * 1. 纯数据提供 —— 对外只暴露 List<Video>，不暴露生成逻辑
 * 2. 每条视频独立创建 —— 修改任意一条不影响其他数据
 * 3. 全参构造器 + newVideo() 工厂方法 —— 调用方不关心工厂细节
 * 4. 每个视频使用不同的URL，确保视频内容各不相同
 */
public class MockVideoData {

    private static List<Video> cachedVideos;

    // ========== 工厂方法 ==========
    // 视频URL列表 —— 全部 HTTPS，经过实测验证可正常返回 200 的稳定源
    private static final String[] VIDEO_URLS = {
            "https://www.w3schools.com/html/mov_bbb.mp4",
            "https://www.w3schools.com/html/movie.mp4",
            "https://vjs.zencdn.net/v/oceans.mp4",
            "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4",
            "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4",
            "https://test-videos.co.uk/vids/jellyfish/mp4/h264/720/Jellyfish_720_10s_1MB.mp4",
            "https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4",
            "https://bdcloud-player-new.cdn.bcebos.com/testvideo/mp4/360video/ThaiKongfu.mp4",
            "https://sf1-cdn-tos.huoshanstatic.com/obj/media-fe/xgplayer_doc_video/mp4/xgplayer-demo-360p.mp4",
    };

    // 6个固定的纯色 —— 用于本地生成头像，不依赖网络
    private static final int[] AVATAR_COLORS = {
            0xFFE91E63, 0xFF2196F3, 0xFF4CAF50, 0xFFFF9800, 0xFF9C27B0, 0xFF00BCD4
    };

    private static Video newVideo(String id, int urlIndex, String title, String author,
                                  int like, int comment, int collect, int share) {
        String url = VIDEO_URLS[urlIndex % VIDEO_URLS.length];
        int avatarColor = AVATAR_COLORS[urlIndex % AVATAR_COLORS.length];
        return new Video(
                id,
                url,
                title,
                author,
                String.valueOf(avatarColor),
                like, comment, collect, share,
                "",
                url,
                url
        );
    }

    // ========== 对外唯一入口 ==========

    public static List<Video> getMockVideos() {
        if (cachedVideos != null) {
            return new ArrayList<>(cachedVideos);
        }

        List<Video> list = new ArrayList<>();

        // ---- 每个视频使用不同的URL，视频内容各不相同 ----

        list.add(newVideo("video_1", 0, "大兔子先生的奇幻冒险之旅", "动画工房",
                52340, 3840, 12400, 860));

        list.add(newVideo("video_2", 1, "大象的梦想 - 动画短片", "梦想工作室",
                89100, 6200, 25000, 2100));

        list.add(newVideo("video_3", 2, "熊熊燃烧的激情岁月", "燃烧视觉",
                45600, 3100, 18900, 1200));

        list.add(newVideo("video_4", 3, "更大规模的逃脱计划行动", "逃脱大师",
                120500, 9100, 43500, 4500));

        list.add(newVideo("video_5", 4, "欢乐时光无限精彩剪辑", "欢乐制造机",
                34500, 2300, 11200, 780));

        list.add(newVideo("video_6", 5, "刺激的驾乘体验实录", "速度与激情",
                67800, 4500, 21000, 1800));

        list.add(newVideo("video_7", 6, "冰川融化的警示纪录片", "冰川记录者",
                98000, 7200, 34000, 5200));

        list.add(newVideo("video_8", 7, "精灵与剑的传说故事", "剑与魔法",
                56000, 3900, 19800, 1500));

        list.add(newVideo("video_9", 8, "斯巴鲁傲虎街道与越野测评", "汽车评测官",
                78900, 5600, 28000, 3200));

        list.add(newVideo("video_10", 9, "钢铁之泪 - 科幻巨作短片", "科幻影院",
                134000, 10500, 56200, 8900));

        list.add(newVideo("video_11", 10, "大众GTI深度评测体验", "专业车评",
                45600, 3800, 15600, 2100));

        list.add(newVideo("video_12", 11, "疯狂越野之旅全程记录", "越野探险家",
                67000, 4800, 23000, 3400));

        list.add(newVideo("video_13", 12, "一万元能买什么车？实测", "实惠购车指南",
                89000, 7200, 31000, 5600));

        list.add(newVideo("video_14", 13, "大兔子先生的奇幻冒险续集", "动画工坊",
                41000, 2900, 13400, 1100));

        list.add(newVideo("video_15", 14, "大象的梦想 - 重制版全集", "梦想动画",
                96000, 8100, 37000, 4800));

        list.add(newVideo("video_16", 15, "美食探店 - 隐藏在巷子里的川味", "美食家小红",
                125000, 9800, 45000, 6700));

        list.add(newVideo("video_17", 16, "旅行Vlog - 云南大理七日游", "旅行者小明",
                187000, 13200, 78000, 12500));

        list.add(newVideo("video_18", 17, "舞蹈教学入门 - 零基础学街舞", "舞蹈精灵小美",
                98000, 6700, 35000, 4300));

        list.add(newVideo("video_19", 18, "宠物日常 - 我家柯基的快乐一天", "宠物博主阿花",
                213000, 18500, 96000, 23000));

        list.add(newVideo("video_20", 19, "美妆教程 - 三分钟快速出门妆", "穿搭女王",
                156000, 11500, 62000, 8900));

        list.add(newVideo("video_21", 20, "数码评测 - 新旗舰手机深度体验", "科技达人阿强",
                234000, 19200, 89000, 15600));

        list.add(newVideo("video_22", 21, "极限运动集锦 - 滑板少年的街头", "极限挑战者",
                112000, 7800, 41000, 5400));

        list.add(newVideo("video_23", 22, "街头采访 - 路人眼中的爱情观", "街头艺术家",
                89000, 6100, 28000, 3700));

        list.add(newVideo("video_24", 23, "生活妙招 - 十个厨房清洁小技巧", "家居改造王",
                178000, 14500, 67000, 11200));

        list.add(newVideo("video_25", 24, "摄影教学 - 手机也能拍出大片感", "摄影大师",
                145000, 10200, 54000, 7800));

        list.add(newVideo("video_26", 25, "音乐弹奏 - 指弹吉他版告白气球", "音乐梦想家",
                201000, 16800, 83000, 14500));

        list.add(newVideo("video_27", 26, "手工制作 - 废纸盒变身收纳神器", "手工匠人",
                167000, 13400, 61000, 9800));

        list.add(newVideo("video_28", 27, "游戏实况 - 新游开荒全程解说", "游戏高手",
                256000, 21500, 97000, 18900));

        list.add(newVideo("video_29", 28, "读书分享 - 三本改变人生的好书", "读书人",
                134000, 9800, 48000, 6500));

        list.add(newVideo("video_30", 29, "穿搭灵感 - 春季通勤搭配一周不重样", "穿搭女王",
                189000, 15200, 72000, 11300));

        list.add(newVideo("video_31", 30, "家居改造 - 出租屋也能变精致小窝", "家居改造王",
                223000, 19800, 94000, 17800));

        list.add(newVideo("video_32", 31, "职场心得 - 互联网大厂三年经验分享", "数码先锋",
                98000, 7300, 32000, 4100));

        list.add(newVideo("video_33", 32, "情感故事 - 十年异地恋的坚持与结局", "情感导师",
                345000, 32000, 156000, 42000));

        list.add(newVideo("video_34", 33, "科普冷知识 - 你可能不知道的十个事实", "科普君",
                278000, 24500, 112000, 21000));

        list.add(newVideo("video_35", 34, "厨艺教学 - 年夜饭硬菜东坡肉完整教程", "厨艺大师",
                190000, 15600, 75000, 13200));

        list.add(newVideo("video_36", 35, "健身入门 - 在家也能练出马甲线", "健身教练",
                312000, 28500, 134000, 25600));

        list.add(newVideo("video_37", 36, "汽车试驾 - 特斯拉Model3山路实测", "汽车评测官",
                156000, 11800, 54000, 8900));

        list.add(newVideo("video_38", 37, "爆笑集锦 - 本年度最搞笑失误视频合集", "欢乐制造机",
                456000, 43000, 198000, 56000));

        list.add(newVideo("video_39", 38, "穿越题材短剧 - 带着手机回古代", "穿越剧场",
                234000, 19200, 86000, 14500));

        list.add(newVideo("video_40", 39, "短剧全集 - 霸总的意外恋人", "霸总剧场",
                387000, 35000, 167000, 38000));

        list.add(newVideo("video_41", 40, "逆袭人生 - 从普通职员到公司合伙人", "职场导师",
                198000, 16200, 78000, 13200));

        list.add(newVideo("video_42", 41, "闪婚玫瑰 - 先婚后爱的浪漫故事", "浪漫剧场",
                432000, 41000, 205000, 49000));

        list.add(newVideo("video_43", 42, "东北爱情故事 - 在冰天雪地里遇见你", "东北影视",
                267000, 23500, 112000, 21000));

        list.add(newVideo("video_44", 43, "赘婿翻身记 - 被瞧不起的人站起来了", "励志剧场",
                523000, 48000, 245000, 67000));

        list.add(newVideo("video_45", 44, "重生之商业帝国 - 回到十年前重新开始", "重生剧场",
                389000, 34500, 178000, 42000));

        list.add(newVideo("video_46", 45, "热门 - 宠物猫的搞笑日常合集", "宠物博主阿花",
                345000, 32000, 156000, 28000));

        list.add(newVideo("video_47", 46, "科技前沿 - 2026年AI将改变哪些行业", "科技达人阿强",
                298000, 27500, 134000, 23500));

        list.add(newVideo("video_48", 47, "美食Vlog - 广州早茶一条街吃到爽", "美食家小红",
                210000, 17800, 89000, 14500));

        list.add(newVideo("video_49", 48, "游戏实况 - 魂系新作的百次死亡之旅", "游戏高手",
                378000, 34000, 167000, 31000));

        list.add(newVideo("video_50", 49, "旅行Vlog - 北海道冬天的极致浪漫", "旅行者小明",
                267000, 24000, 123000, 21000));

        list.add(newVideo("video_51", 50, "数码开箱 - 最新降噪耳机横评对比", "数码先锋",
                123000, 9500, 45000, 6700));

        list.add(newVideo("video_52", 51, "手工教程 - 用废木板做个性书架", "手工匠人",
                89000, 6700, 34000, 4500));

        list.add(newVideo("video_53", 52, "舞蹈高阶 - Breaking地板技巧教学", "舞蹈精灵小美",
                145000, 11200, 56000, 7800));

        list.add(newVideo("video_54", 53, "美妆测评 - 十款平价口红真实试色", "穿搭女王",
                198000, 16500, 78000, 12300));

        list.add(newVideo("video_55", 54, "摄影技巧 - 如何拍出唯美逆光人像", "摄影大师",
                112000, 8900, 41000, 5600));

        list.add(newVideo("video_56", 55, "音乐弹唱 - 原创歌曲创作全过程", "音乐梦想家",
                156000, 12300, 61000, 8900));

        list.add(newVideo("video_57", 56, "极限运动 - 翼装飞行第一视角实录", "极限挑战者",
                345000, 31000, 156000, 32000));

        list.add(newVideo("video_58", 57, "科普动画 - 黑洞到底长什么样", "科普君",
                289000, 26000, 134000, 23000));

        list.add(newVideo("video_59", 58, "厨艺挑战 - 100元做四菜一汤", "厨艺大师",
                167000, 13400, 65000, 9800));

        list.add(newVideo("video_60", 59, "健身进阶 - 杠铃深蹲完整技术分析", "健身教练",
                234000, 19800, 95000, 16700));

        list.add(newVideo("video_61", 60, "读书感悟 - 活着带给我的人生启示", "读书人",
                89000, 6500, 31000, 4200));

        list.add(newVideo("video_62", 61, "家居DIY - 旧物翻新大改造全记录", "家居改造王",
                145000, 11500, 56000, 7800));

        list.add(newVideo("video_63", 62, "汽车对比 - 30万预算买油车还是电车", "汽车评测官",
                189000, 15600, 72000, 11500));

        list.add(newVideo("video_64", 63, "动画短片 - 都市孤独症的故事", "动画工坊",
                98000, 7200, 36000, 5400));

        list.add(newVideo("video_65", 64, "搞笑短剧 - 办公室搞笑日常第三季", "欢乐制造机",
                278000, 25000, 123000, 21000));

        list.add(newVideo("video_66", 65, "热血漫剪 - 年度最燃动漫剪辑合集", "燃烧视觉",
                456000, 42000, 210000, 54000));

        list.add(newVideo("video_67", 66, "温情短片 - 父亲写给女儿的一封信", "冰川记录者",
                389000, 36000, 178000, 45000));

        list.add(newVideo("video_68", 67, "治愈系Vlog - 一个人的周末慢生活", "摄影大师",
                198000, 16200, 82000, 13400));

        list.add(newVideo("video_69", 68, "科幻短片 - AI觉醒的第一天发生了什么", "科幻影院",
                312000, 28500, 145000, 27800));

        list.add(newVideo("video_70", 69, "极限越野 - 穿越可可西里无人区", "越野探险家",
                267000, 24000, 123000, 21000));

        list.add(newVideo("video_71", 70, "剑道修行 - 日本古剑术入门体验", "剑与魔法",
                89000, 6400, 31000, 4200));

        list.add(newVideo("video_72", 71, "逃脱游戏 - 密室逃脱通关全纪录", "逃脱大师",
                134000, 10200, 48000, 6500));

        list.add(newVideo("video_73", 72, "购车指南 - 家庭第一辆车该怎么选", "实惠购车指南",
                178000, 14500, 67000, 9800));

        list.add(newVideo("video_74", 73, "动画连载 - 大兔子先生第三季第一集", "动画工房",
                234000, 19800, 95000, 15600));

        list.add(newVideo("video_75", 74, "梦想纪实 - 普通人的非凡人生故事", "梦想工作室",
                167000, 13400, 64000, 8900));

        list.add(newVideo("video_76", 75, "宠物养成 - 两个月大的金毛到家第一天", "宠物博主阿花",
                345000, 32000, 156000, 28000));

        list.add(newVideo("video_77", 76, "数码前沿 - 折叠屏手机的第三次革命", "科技达人阿强",
                212000, 17800, 89000, 14800));

        list.add(newVideo("video_78", 77, "旅行攻略 - 泰国清迈1000元玩三天", "旅行者小明",
                298000, 27000, 134000, 23000));

        list.add(newVideo("video_79", 78, "情感疗愈 - 分手后如何走出低谷期", "情感导师",
                423000, 39000, 189000, 45000));

        list.add(newVideo("video_80", 79, "职场进阶 - 面试时绝对不能说的五句话", "数码先锋",
                267000, 23000, 112000, 17800));

        cachedVideos = list;
        return new ArrayList<>(cachedVideos);
    }

    /**
     * 根据关键词搜索视频
     * @param keyword 搜索关键词
     * @return 匹配的视频列表
     */
    public static List<Video> searchVideos(String keyword) {
        List<Video> allVideos = getMockVideos();
        List<Video> results = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        String lowerKeyword = keyword.toLowerCase();
        for (Video video : allVideos) {
            if (video.getTitle().toLowerCase().contains(lowerKeyword) ||
                    video.getAuthor().toLowerCase().contains(lowerKeyword)) {
                results.add(video);
            }
        }

        return results;
    }

    /**
     * 获取推荐关键词列表
     * @return 推荐关键词数组
     */
    public static String[] getRecommendKeywords() {
        return new String[]{
                "大兔子",
                "大象",
                "冒险",
                "汽车评测",
                "美食探店",
                "旅行Vlog",
                "宠物日常",
                "美妆教程",
                "数码评测",
                "游戏实况",
                "健身",
                "舞蹈教学",
                "街头采访",
                "家居改造",
                "摄影技巧",
                "音乐弹唱",
                "手工制作",
                "读书分享",
                "情感故事",
                "科普冷知识"
        };
    }

    /**
     * 获取相关搜索关键词（底部轮播用）
     * @return 相关搜索关键词数组
     */
    public static String[] getRelatedSearchKeywords() {
        return new String[]{
                "东北爱情故事", "闪婚玫瑰", "短剧", "逆袭",
                "霸总", "穿越", "重生", "赘婿",
                "美食探店", "旅行Vlog", "数码评测", "宠物日常"
        };
    }

    /**
     * 获取图片卡片数据（视频流混排用）
     * @return 图片卡片列表
     */
    public static List<ImageCard> getImageCards() {
        List<ImageCard> cards = new ArrayList<>();

        // 每个图片卡片包含 3 张图，支持左右滑动浏览
        String[][] imageSets = {
                {"https://picsum.photos/seed/card1a/300/400", "https://picsum.photos/seed/card1b/300/400", "https://picsum.photos/seed/card1c/300/400"},
                {"https://picsum.photos/seed/card2a/300/400", "https://picsum.photos/seed/card2b/300/400", "https://picsum.photos/seed/card2c/300/400"},
                {"https://picsum.photos/seed/card3a/300/400", "https://picsum.photos/seed/card3b/300/400", "https://picsum.photos/seed/card3c/300/400"},
                {"https://picsum.photos/seed/card4a/300/400", "https://picsum.photos/seed/card4b/300/400", "https://picsum.photos/seed/card4c/300/400"},
                {"https://picsum.photos/seed/card5a/300/400", "https://picsum.photos/seed/card5b/300/400", "https://picsum.photos/seed/card5c/300/400"},
                {"https://picsum.photos/seed/card6a/300/400", "https://picsum.photos/seed/card6b/300/400", "https://picsum.photos/seed/card6c/300/400"},
        };

        for (int i = 0; i < imageSets.length; i++) {
            ImageCard card = new ImageCard();
            List<String> urls = new ArrayList<>();
            for (String url : imageSets[i]) {
                urls.add(url);
            }
            card.setImageUrls(urls);
            card.setImageUrl(imageSets[i][0]); // 兼容旧代码
            card.setTitle("推荐图集 " + (i + 1) + " (" + imageSets[i].length + "图)");
            cards.add(card);
        }
        return cards;
    }

    /**
     * 分页获取视频列表
     * @param page     页码（从0开始）
     * @param pageSize 每页大小
     * @return 当前页的视频列表
     */
    public static List<Video> getVideosByPage(int page, int pageSize) {
        List<Video> all = getMockVideos();
        int start = page * pageSize;
        if (start >= all.size()) return new ArrayList<>();
        int end = Math.min(start + pageSize, all.size());
        return new ArrayList<>(all.subList(start, end));
    }

    /**
     * 判断是否还有更多页
     * @param page     当前页码
     * @param pageSize 每页大小
     * @return 是否有更多数据
     */
    public static boolean hasMorePages(int page, int pageSize) {
        List<Video> all = getMockVideos();
        int totalPages = (int) Math.ceil((double) all.size() / pageSize);
        return page < totalPages - 1;
    }
}
