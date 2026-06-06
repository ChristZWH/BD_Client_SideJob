package com.example.bd_client_sidejob.data.local;

import com.example.bd_client_sidejob.data.model.ImageCard;
import com.example.bd_client_sidejob.data.model.Video;

import java.util.ArrayList;
import java.util.List;

// 15条视频假数据
public class MockVideoData {

    private static final String[] VIDEO_URLS = {
            "https://www.w3schools.com/html/mov_bbb.mp4",
            "http://vjs.zencdn.net/v/oceans.mp4",
            "https://media.w3.org/2010/05/sintel/trailer_hd.mp4",
            "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4",
            "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4",
            "https://www.w3schools.com/html/mov_bbb.mp4",
            "http://vjs.zencdn.net/v/oceans.mp4",
            "https://media.w3.org/2010/05/sintel/trailer_hd.mp4",
            "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4",
            "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4",
            "https://www.w3schools.com/html/mov_bbb.mp4",
            "http://vjs.zencdn.net/v/oceans.mp4",
            "https://media.w3.org/2010/05/sintel/trailer_hd.mp4",
            "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4",
            "https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4"
    };

    // 是否使用本地视频（设置为 true 使用本地资源，false 使用网络视频）
    private static final boolean USE_LOCAL_VIDEOS = false;
    // 本地视频资源名称（不带扩展名）
    private static final String LOCAL_VIDEO_RESOURCE_NAME = "sample_video";

    private static final String[] COVER_URLS = {
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerEscapes.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerFun.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerJoyrides.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerMeltdowns.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/SubaruOutbackOnStreetAndDirt.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/VolkswagenGTIReview.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/WeAreGoingOnBullrun.jpg",
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/WhatCarCanYouGetForAGrand.jpg",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg"
    };

    private static final String[] AVATAR_URLS = {
            "https://picsum.photos/seed/user1/100/100",
            "https://picsum.photos/seed/user2/100/100",
            "https://picsum.photos/seed/user3/100/100",
            "https://picsum.photos/seed/user4/100/100",
            "https://picsum.photos/seed/user5/100/100",
            "https://picsum.photos/seed/user6/100/100",
            "https://picsum.photos/seed/user7/100/100",
            "https://picsum.photos/seed/user8/100/100",
            "https://picsum.photos/seed/user9/100/100",
            "https://picsum.photos/seed/user10/100/100",
            "https://picsum.photos/seed/user11/100/100",
            "https://picsum.photos/seed/user12/100/100",
            "https://picsum.photos/seed/user13/100/100",
            "https://picsum.photos/seed/user14/100/100",
            "https://picsum.photos/seed/user15/100/100"
    };

    private static final String[] TITLES = {
            "大兔子先生的奇幻冒险之旅",
            "大象的梦想 - 动画短片",
            "熊熊燃烧的激情",
            "更大规模的逃脱计划",
            "欢乐时光无限",
            "刺激的驾乘体验",
            "冰川融化的警示",
            "精灵与剑的传说",
            "斯巴鲁傲虎街道与越野",
            "钢铁之泪 - 科幻短片",
            "大众GTI深度评测",
            "疯狂越野之旅",
            "一万元能买什么车？",
            "大兔子先生的奇幻冒险",
            "大象的梦想 - 重制版"
    };

    private static final String[] AUTHORS = {
            "动画工房",
            "梦想工作室",
            "燃烧视觉",
            "逃脱大师",
            "欢乐制造机",
            "速度与激情",
            "冰川记录者",
            "剑与魔法",
            "汽车评测官",
            "科幻影院",
            "专业车评",
            "越野探险家",
            "实惠购车指南",
            "动画工坊",
            "梦想动画"
    };

    public static List<Video> getMockVideos() {
        List<Video> videos = new ArrayList<>();
        for (int i = 0; i < 15; i++) {  // 这里是写死的数据，一共有15条
            Video video = new Video();
            video.setVideoId("video_" + (i + 1));
            
            // 根据配置选择使用本地视频还是网络视频
            String videoUrl;
            if (USE_LOCAL_VIDEOS) { // 当前不使用本地资源
                // 使用本地资源文件
                videoUrl = "android.resource://com.example.bd_client_sidejob/raw/" + LOCAL_VIDEO_RESOURCE_NAME;
            } else {
                // 使用网络视频
                videoUrl = VIDEO_URLS[i % VIDEO_URLS.length];
            }
            video.setUrl(videoUrl);
            
            video.setTitle(TITLES[i]);
            video.setAuthor(AUTHORS[i]);
            video.setAvatar(AVATAR_URLS[i]);
            video.setCoverUrl(COVER_URLS[i % COVER_URLS.length]);
            video.setLikeCount(generateRandomCount(1000, 100000));
            video.setCommentCount(generateRandomCount(100, 10000));
            video.setCollectCount(generateRandomCount(500, 50000));
            video.setShareCount(generateRandomCount(50, 5000));
            video.setQuality360p(videoUrl);
            video.setQuality720p(videoUrl);
            videos.add(video);
        }
        return videos;
    }

    /**
     * 检查是否有更多页面
     * @param page 当前页码
     * @param pageSize 每页数量
     * @return true 表示还有更多数据
     */
    public static boolean hasMorePages(int page, int pageSize) {
        int totalVideos = 15; // 总视频数
        int totalPages = (int) Math.ceil((double) totalVideos / pageSize);
        return page < totalPages - 1;
    }

    /**
     * 搜索视频
     * @param keyword 搜索关键词
     * @return 匹配的视频列表
     */
    public static List<Video> searchVideos(String keyword) {
        List<Video> allVideos = getMockVideos();
        List<Video> results = new ArrayList<>();
        
        if (keyword == null || keyword.isEmpty()) {
            return allVideos;
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
     * 获取推荐关键词
     * @return 关键词数组
     */
    public static String[] getRecommendKeywords() {
        return new String[]{
                "热门视频",
                "搞笑",
                "科技",
                "美食",
                "旅行",
                "音乐",
                "游戏",
                "教育"
        };
    }

    /**
     * 获取图片卡片列表
     * @return 图片卡片列表
     */
    public static List<ImageCard> getImageCards() {
        return getMockImageCards();
    }

    public static List<Video> getVideosByPage(int page, int pageSize) {
        List<Video> allVideos = getMockVideos();
        int startIndex = page * pageSize;
        int endIndex = Math.min(startIndex + pageSize, allVideos.size());
        
        if (startIndex >= allVideos.size()) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(allVideos.subList(startIndex, endIndex));
    }

    private static int generateRandomCount(int min, int max) {
        return min + (int) (Math.random() * (max - min + 1));
    }

    public static List<ImageCard> getMockImageCards() {
        List<ImageCard> cards = new ArrayList<>();
        String[] urls = {
                "https://picsum.photos/seed/img1/300/200",
                "https://picsum.photos/seed/img2/300/200",
                "https://picsum.photos/seed/img3/300/200",
                "https://picsum.photos/seed/img4/300/200",
                "https://picsum.photos/seed/img5/300/200",
                "https://picsum.photos/seed/img6/300/200"
        };
        
        for (int i = 0; i < urls.length; i++) {
            ImageCard card = new ImageCard();
            card.setImageUrl(urls[i]);
            card.setTitle("推荐内容 " + (i + 1));
            cards.add(card);
        }
        
        return cards;
    }
}