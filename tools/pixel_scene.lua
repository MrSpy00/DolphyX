local ROOT = app.fs.currentPath
local RES = ROOT .. "/app/src/main/res/drawable-nodpi"
local ART = ROOT .. "/tools/art"
app.fs.makeDirectory(RES)
app.fs.makeDirectory(ART)

local W, H = 480, 184
local spr = Sprite(W, H, ColorMode.RGB)
spr.layers[1].name = "scene"
local cel = spr:newCel(spr.layers[1], 1, Image(W, H), Point(0, 0))
local img = cel.image

local function px(x, y, c)
  if x >= 0 and y >= 0 and x < W and y < H then img:drawPixel(x, y, c) end
end

local function col(v, a)
  return app.pixelColor.rgba(v, v, v, a or 255)
end

local BG = col(10)
local SKY = col(16)
local DARK = col(28)
local MID = col(58)
local LIGHT = col(120)
local PALE = col(176)
local WHITE = col(232)

local function rect(x0, y0, x1, y1, c)
  for y = y0, y1 do
    for x = x0, x1 do px(x, y, c) end
  end
end

local function outline(x0, y0, x1, y1, c)
  for x = x0, x1 do px(x, y0, c); px(x, y1, c) end
  for y = y0, y1 do px(x0, y, c); px(x1, y, c) end
end

local function ellipse(cx, cy, rx, ry, c)
  for y = -ry, ry do
    for x = -rx, rx do
      if (x * x) / (rx * rx) + (y * y) / (ry * ry) <= 1 then px(cx + x, cy + y, c) end
    end
  end
end

-- canvas
rect(0, 0, W - 1, H - 1, BG)

-- window recess
rect(24, 18, 300, 128, SKY)
rect(24, 18, 300, 20, LIGHT)
rect(24, 126, 300, 128, LIGHT)

-- stars
math.randomseed(7)
for i = 1, 26 do
  local sx = 32 + math.random(0, 256)
  local sy = 28 + math.random(0, 54)
  px(sx, sy, WHITE)
  if i % 5 == 0 then px(sx + 1, sy, PALE); px(sx, sy + 1, PALE) end
end

-- moon
ellipse(262, 48, 12, 12, WHITE)
ellipse(258, 44, 3, 3, PALE)
ellipse(266, 52, 2, 2, PALE)
px(254, 52, PALE)

-- skyline
local buildings = {
  { 32, 96, 58 }, { 42, 78, 74 }, { 68, 88, 60 }, { 96, 66, 86 },
  { 120, 92, 70 }, { 150, 72, 92 }, { 178, 84, 64 }, { 204, 60, 80 },
  { 230, 88, 72 }, { 254, 74, 96 }, { 282, 94, 62 },
}
for _, b in ipairs(buildings) do
  local x0, top, w = b[1], b[2], b[3]
  rect(x0, top, x0 + w, 126, DARK)
  rect(x0, top, x0 + w, top + 1, MID)
  -- lit windows
  local wy = top + 8
  while wy < 118 do
    local wx = x0 + 6
    while wx < x0 + w - 4 do
      if math.random(0, 2) > 0 then
        rect(wx, wy, wx + 2, wy + 2, WHITE)
        if math.random(0, 1) == 0 then px(wx + 1, wy + 1, PALE) end
      end
      wx = wx + 9
    end
    wy = wy + 11
  end
end

-- antenna on tallest roof
rect(240, 44, 241, 60, LIGHT)
px(240, 42, WHITE); px(239, 43, WHITE); px(242, 43, WHITE)

-- window frame
outline(24, 18, 300, 128, PALE)
outline(22, 16, 302, 130, DARK)
rect(160, 18, 163, 128, PALE)
rect(24, 70, 300, 72, MID)
-- window sill
rect(18, 130, 306, 135, LIGHT)
rect(18, 136, 306, 137, DARK)

-- desk
rect(0, 136, W - 1, H - 1, MID)
rect(0, 136, W - 1, 139, PALE)
rect(0, 140, W - 1, 142, LIGHT)
rect(0, 172, W - 1, H - 1, DARK)
-- desk grain
for i = 0, 12 do
  local y = 146 + i * 2
  for x = 10 + i * 17, 60 + i * 31 do
    if x < W and y < 172 then px(x, y, LIGHT) end
  end
end

-- monitor
rect(334, 50, 466, 132, PALE)
rect(338, 54, 462, 128, col(14))
rect(336, 52, 464, 130, MID)
outline(334, 50, 466, 132, WHITE)
-- screen scanlines
for y = 58, 124, 3 do
  rect(342, y, 458, y, col(26))
end
-- terminal content
rect(348, 62, 452, 64, col(40))
rect(348, 66, 400, 68, WHITE)
rect(404, 66, 412, 68, col(90))
rect(348, 72, 388, 74, col(90))
rect(348, 78, 430, 80, PALE)
rect(348, 84, 372, 86, col(90))
rect(348, 90, 444, 92, col(90))
-- cursor
rect(348, 98, 356, 104, WHITE)
-- waveform on screen
for x = 368, 452 do
  local y = 104 + math.floor(math.sin(x / 6.0) * 6)
  px(x, y, WHITE)
  px(x, y + 1, PALE)
end
-- monitor badge
rect(438, 118, 452, 124, WHITE)
-- stand
rect(382, 132, 418, 136, LIGHT)
rect(390, 136, 410, 142, MID)
rect(372, 142, 428, 146, DARK)

-- keyboard
rect(212, 146, 322, 166, col(38))
outline(212, 146, 322, 166, col(72))
local ky = 150
while ky <= 160 do
  local kx = 218
  while kx <= 314 do
    rect(kx, ky, kx + 7, ky + 3, PALE)
    rect(kx, ky + 3, kx + 7, ky + 3, col(90))
    kx = kx + 11
  end
  ky = ky + 6
end
rect(240, 162, 296, 164, col(72))

-- mug
rect(304, 122, 322, 140, col(86))
rect(304, 122, 322, 123, WHITE)
outline(304, 122, 322, 140, PALE)
rect(322, 126, 328, 136, col(86))
outline(322, 126, 328, 136, PALE)
-- steam
px(312, 118, PALE); px(313, 116, PALE); px(312, 114, col(90))
px(316, 118, PALE); px(315, 116, PALE)

-- dolphin sitting at the desk
local function dolphin()
  -- dorsal fin
  rect(86, 74, 89, 79, LIGHT)
  rect(82, 79, 89, 86, MID)
  -- head
  ellipse(92, 98, 22, 20, WHITE)
  -- left shading
  for y = 80, 118 do
    for x = 70, 86 do
      local dx, dy = (x - 92) / 22.0, (y - 98) / 20.0
      if dx * dx + dy * dy <= 1 and x < 80 then px(x, y, LIGHT) end
    end
  end
  -- beak
  for i = 0, 6 do
    rect(110 + i, 100 + math.floor(i / 3), 136, 107 + math.floor(i / 4), WHITE)
  end
  rect(110, 108, 132, 111, WHITE)
  rect(114, 111, 130, 112, LIGHT)
  for x = 118, 132 do px(x, 113, DARK) end
  -- eye
  rect(98, 92, 103, 98, col(8))
  rect(99, 93, 100, 94, WHITE)
  rect(96, 89, 105, 90, LIGHT)
  -- body
  rect(72, 116, 116, 158, WHITE)
  rect(72, 116, 79, 158, LIGHT)
  rect(72, 150, 116, 158, MID)
  rect(108, 120, 110, 148, LIGHT)
  -- fins
  rect(66, 124, 74, 146, MID)
  rect(64, 132, 69, 150, LIGHT)
  rect(114, 120, 120, 142, LIGHT)
  rect(118, 128, 122, 146, MID)
  -- rim light
  for x = 76, 112 do px(x, 116, WHITE) end
end
dolphin()

-- desk shadow under dolphin and keyboard
for x = 64, 330 do
  if x < 130 or x > 210 then px(x, 159, col(40)) end
end

-- scanlines (subtle)
for y = 0, H - 1, 3 do
  for x = 0, W - 1 do
    px(x, y, col(0, 14))
  end
end

-- corner vignette
for i = 0, 12 do
  for x = i * 2, i * 2 + 1 do
    px(x, H - 1 - i, BG)
    px(W - 1 - x, H - 1 - i, BG)
  end
end

spr.filename = ART .. "/px_scene.aseprite"
spr:saveAs(ART .. "/px_scene.aseprite")
app.command.ExportSpriteSheet{
  ui = false,
  type = SpriteSheetType.HORIZONTAL,
  textureFilename = RES .. "/px_scene.png",
  dataFilename = ART .. "/px_scene.json",
  dataFormat = SpriteSheetDataFormat.JSON_ARRAY,
  trim = false, borderPadding = 0, shapePadding = 0, innerPadding = 0,
}
print("scene ok")

