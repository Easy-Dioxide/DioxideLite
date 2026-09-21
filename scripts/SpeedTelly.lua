--[[
DioxideLite 2.1.1 Lua Script
Script Name: SpeedTelly_Scaffold
Mode: GreenPlayer-like SpeedTelly Bridging

核心优化：
1. AIM 阶段：稳定锁瞄准点，但带微小人手抖动和缓慢收敛，不机械钉死
2. FORWARD_RESET 阶段：放置后视角向前摆正疾跑，但转头使用人类鼠标加速度曲线
3. 禁止一帧大角度转头：每 tick 严格限制 Yaw/Pitch 最大步长
4. 落点有效性校验：目标必须在地面/可放置区域，防止自走虚空
5. 保留右键启动、WASD 原生物理、移动修复、射线放置、角度保护
]]

local module = dioxidelite.module({
    name = "SpeedTelly_Scaffold",
    id = "speedtelly_scaffold",
    category = "Movement"
})

-- GUI可调参数
local maxTurnPerTick      = module:number("MaxTurnPerTick(°)", 11, 1, 25, 0.5)
local turnEase            = module:number("Turn Ease", 2.1, 1.0, 4.0, 0.1)
local humanJitter         = module:number("Human Jitter (unused)", 0, 0, 0, 0.05)
local autoSprint          = module:boolean("Auto Sprint", true)
local groundOnly          = module:boolean("Ground Only", true)
local enableHeadTurn      = module:boolean("Telly View Lock", true)
local moveSmoothFactor    = module:number("MoveSmooth", 0.72, 0.0, 0.95, 0.01)
local placeBlockRange     = module:number("Place Range", 4.3, 2.0, 6.0, 0.1)
local placeCooldown       = module:number("Place Cooldown(tick)", 2, 1, 10, 1)
local renderAimMarker     = module:boolean("Render Aim Marker(HUD)", true)
local maxAngleError       = module:number("Max Allow Angle Error(°)", 2.6, 0.5, 8, 0.1)
local forwardHoldTicks    = module:number("Forward Hold Ticks", 2, 1, 5, 1)
local resetYawMaxStep     = module:number("Reset Turn Step(°)", 9, 2, 20, 0.5)
local validRangeCheck     = module:boolean("Valid Place Check", true)

local RAD = math.pi / 180
local cachedDirX = 0.0
local cachedDirZ = 0.0
local placeTickTimer = 0
local targetScreenX = 0
local targetScreenY = 0

local state = "AIM"
local forwardTickCount = 0
local resetProgress = 0.0
local resetStartYaw = 0.0
local resetTargetYaw = 0.0
local resetStartPitch = 0.0
local resetTargetPitch = 0.0

local function wrapAngle180(angle)
    angle = angle % 360
    if angle > 180 then angle = angle - 360 end
    if angle < -180 then angle = angle + 360 end
    return angle
end

-- 基础角度限幅步进，防止一帧大角度转头
local function angleStep(current, target, maxStep)
    local delta = wrapAngle180(target - current)
    local absDelta = math.abs(delta)
    if absDelta <= maxStep then
        return target, absDelta
    end
    return current + math.sign(delta) * maxStep, maxStep
end

-- AIM 阶段：稳定锁方块，但带平滑收敛和微抖
local function smoothstep(t)
    t = math.max(0.0, math.min(1.0, t))
    return t * t * (3.0 - 2.0 * t)
end

local function smoothAngle(current, target, maxStep, response)
    local delta = wrapAngle180(target - current)
    local distance = math.abs(delta)

    if distance < 0.05 then
        return target
    end

    local normalized = math.min(1.0, distance / 45.0)
    local curve = smoothstep(normalized)

    local step = maxStep * (0.25 + curve * 0.75)
    step = math.min(step, distance)

    return current + math.sign(delta) * step * response
end

-- FORWARD_RESET 阶段：带加速度曲线的人类鼠标回正
local function humanResetTurn(current, start, target, progress, maxStep)
    local easedProgress = 1 - math.pow(1 - progress, 2)
    local ideal = start + wrapAngle180(target - start) * easedProgress
    local newVal, _ = angleStep(current, ideal, maxStep)
    return newVal
end

module:on("tick", function()
    if not player:is_available() then
        cachedDirX = 0
        cachedDirZ = 0
        state = "AIM"
        forwardTickCount = 0
        resetProgress = 0
        return
    end

    local plyEnt = player:entity()

    if placeTickTimer > 0 then
        placeTickTimer = placeTickTimer - 1
    end

    local holdRightClick = input:is_down("mouse.right")
    if not holdRightClick then
        cachedDirX = 0
        cachedDirZ = 0
        placeTickTimer = 0
        state = "AIM"
        forwardTickCount = 0
        resetProgress = 0
        return
    end

    if groundOnly:get() and not plyEnt.on_ground then
        cachedDirX = 0
        cachedDirZ = 0
        state = "AIM"
        forwardTickCount = 0
        resetProgress = 0
        return
    end

    local forward = (input:is_down("W") and 1 or 0) - (input:is_down("S") and 1 or 0)
    local strafe = (input:is_down("D") and 1 or 0) - (input:is_down("A") and 1 or 0)

    if forward == 0 and strafe == 0 then
        cachedDirX = 0
        cachedDirZ = 0
        state = "AIM"
        forwardTickCount = 0
        resetProgress = 0
        return
    end

    if autoSprint:get() then
        action:sprint(true)
    end

    local currentYaw = player:yaw()
    local currentPitch = player:pitch()
    local yawRad = currentYaw * RAD

    local rawDirX = forward * (-math.sin(yawRad)) + strafe * math.cos(yawRad)
    local rawDirZ = forward * math.cos(yawRad) + strafe * math.sin(yawRad)
    local length = math.sqrt(rawDirX^2 + rawDirZ^2)
    if length < 1e-5 then
        cachedDirX = 0
        cachedDirZ = 0
        return
    end

    rawDirX = rawDirX / length
    rawDirZ = rawDirZ / length

    -- 移动修复：一阶低通滤波
    local smoothK = moveSmoothFactor:get()
    cachedDirX = cachedDirX * smoothK + rawDirX * (1 - smoothK)
    cachedDirZ = cachedDirZ * smoothK + rawDirZ * (1 - smoothK)

    -- 目标落点
    local targetWorldX = plyEnt.x + cachedDirX * placeBlockRange:get()
    local targetWorldZ = plyEnt.z + cachedDirZ * placeBlockRange:get()
    local targetWorldY = plyEnt.y - 1.0

    local deltaX = targetWorldX - plyEnt.x
    local deltaY = targetWorldY - plyEnt.y
    local deltaZ = targetWorldZ - plyEnt.z

    local targetYaw = math.deg(math.atan2(-deltaX, deltaZ))
    local horizontalDist = math.sqrt(deltaX * deltaX + deltaZ * deltaZ)
    local targetPitch = math.deg(math.atan2(-deltaY, horizontalDist))

    -- 向前回正目标：Yaw 跟随前进方向，Pitch 回到略水平
    local forwardPitch = -7.5

    local yawErr = math.abs(wrapAngle180(targetYaw - currentYaw))
    local pitchErr = math.abs(wrapAngle180(targetPitch - currentPitch))
    local totalAngleError = math.sqrt(yawErr * yawErr + pitchErr * pitchErr)

    -- 落点有效性校验：防止虚空放置
    local placeValid = true
    if validRangeCheck:get() then
        local dist = math.sqrt(deltaX * deltaX + deltaZ * deltaZ)
        if dist > placeBlockRange:get() + 0.6 then
            placeValid = false
        end
        if targetWorldY < plyEnt.y - 2.2 then
            placeValid = false
        end
    end

    -- ========== 状态机：AIM / FORWARD_RESET ==========
    if enableHeadTurn:get() then
        if state == "AIM" then
            local newYaw = smoothAngle(currentYaw, targetYaw, maxTurnPerTick:get(), 1.0)
            local newPitch = smoothAngle(currentPitch, targetPitch, maxTurnPerTick:get() * 0.55, 0.9)
            action:rotate(newYaw, newPitch)

        elseif state == "FORWARD_RESET" then
            forwardTickCount = forwardTickCount + 1
            resetProgress = math.min(1.0, resetProgress + 0.12)

            local newYaw = humanResetTurn(currentYaw, resetStartYaw, resetTargetYaw, resetProgress, resetYawMaxStep:get())
            local newPitch = humanResetTurn(currentPitch, resetStartPitch, resetTargetPitch, resetProgress, resetYawMaxStep:get() * 0.5)

            action:rotate(newYaw, newPitch)

            if forwardTickCount >= forwardHoldTicks:get() then
                state = "AIM"
                forwardTickCount = 0
                resetProgress = 0
            end
        end
    end

    -- HUD 瞄准标记
    if renderAimMarker:get() then
        local screenPos = world:world_to_screen(targetWorldX, targetWorldY, targetWorldZ)
        targetScreenX = screenPos.x
        targetScreenY = screenPos.y
    end

    -- 放置逻辑
    if placeTickTimer <= 0 and state == "AIM" and placeValid then
        if totalAngleError < maxAngleError:get() then
            local traceResult = world:raycast(player:eye_pos(), targetWorldX, targetWorldY, targetWorldZ, placeBlockRange:get())
            if traceResult.hit then
                action:use()
                placeTickTimer = placeCooldown:get()

                -- 放置成功：进入向前回正状态
                state = "FORWARD_RESET"
                forwardTickCount = 0
                resetProgress = 0
                resetStartYaw = currentYaw
                resetTargetYaw = targetYaw
                resetStartPitch = currentPitch
                resetTargetPitch = forwardPitch
            end
        end
    end
end)

-- HUD 渲染
module:on("render", function()
    if not renderAimMarker:get() or not player:is_available() then return end
    render:circle(targetScreenX, targetScreenY, 8, 0xFF39FF14)
    render:line(targetScreenX - 12, targetScreenY, targetScreenX + 12, targetScreenY, 2, 0xFF39FF14)
    render:line(targetScreenX, targetScreenY - 12, targetScreenX, targetScreenY + 12, 2, 0xFF39FF14)
end)

module:on("disable", function()
    action:sprint(false)
    cachedDirX = 0
    cachedDirZ = 0
    placeTickTimer = 0
    state = "AIM"
    forwardTickCount = 0
    resetProgress = 0
end)
