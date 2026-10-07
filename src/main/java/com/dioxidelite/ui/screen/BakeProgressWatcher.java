package com.dioxidelite.ui.screen;

/**
 * 由「需要在每帧渲染后轮询后台烘焙进度」的界面实现。
 *
 * <p>26.1.2 里 {@code Screen} 不再有 {@code render} 方法（改为
 * {@code extractRenderStateWithTooltipAndSubtitles}），且 Mixin 不会向父类查找注入目标，
 * 所以实现方不能自己往父类方法上挂 {@code @Inject}。约定做法：
 * 实现方只声明本接口，由 {@code VanillaScreenThemeMixin}（挂在 {@code Screen} 上、
 * 每帧调用一次）统一转发过来。
 */
public interface BakeProgressWatcher {

    /** 每帧调用一次；实现方内部自行判断是否有烘焙任务在进行。 */
    void dioxideLite$pollBake();
}
