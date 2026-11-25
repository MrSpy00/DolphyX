local ROOT = app.fs.currentPath
local RES = ROOT .. "/app/src/main/res/drawable-nodpi"
local ART = ROOT .. "/tools/art"
app.fs.makeDirectory(RES)
app.fs.makeDirectory(ART)

-- Draw on a 108x108 logical grid and stamp 4x4 blocks for a crisp 432px icon.
local GRID = 108
local SCALE = 4
local W, H = GRID * SCALE, GRID * SCALE
local spr = Sprite(W, H, ColorMode.RGB)
spr.layers[1].name = "logo"
local cel = spr:newCel(spr.layers[1], 1, Image(W, H), Point(0, 0))
local img = cel.image

local function rgba(v, a) return app.pixelColor.rgba(v, v, v, a or 255) end
local WHITE = rgba(244)
local LIGHT = rgba(201)
local MID = rgba(138)
local DARK = rgba(42)
local BLACK = rgba(10)
local CYAN = app.pixelColor.rgba(86, 204, 242, 255)

local function px(x, y, c)
  if x < 0 or y < 0 or x >= GRID or y >= GRID then return end
  local bx, by = x * SCALE, y * SCALE
  for dy = 0, SCALE - 1 do
    for dx = 0, SCALE - 1 do
      img:drawPixel(bx + dx, by + dy, c)
    end
  end
end

local function rect(x0, y0, x1, y1, c)
  for y = y0, y1 do for x = x0, x1 do px(x, y, c) end end
end

local function ellipse(cx, cy, rx, ry, c)
  for y = -ry, ry do
    for x = -rx, rx do
      if (x * x) / (rx * rx) + (y * y) / (ry * ry) <= 1 then px(cx + x, cy + y, c) end
    end
  end
end

local function tri(x1, y1, x2, y2, x3, y3, c)
  local minY = math.min(y1, y2, y3)
  local maxY = math.max(y1, y2, y3)
  for y = minY, maxY do
    local xs = {}
    local function edge(ax, ay, bx, by)
      if ay ~= by and y >= math.min(ay, by) and y <= math.max(ay, by) then
        xs[#xs + 1] = ax + (bx - ax) * (y - ay) / (by - ay)
      end
    end
    edge(x1, y1, x2, y2)
    edge(x2, y2, x3, y3)
    edge(x3, y3, x1, y1)
    if #xs >= 2 then
      table.sort(xs)
      for x = math.floor(xs[1] + 0.5), math.floor(xs[#xs] + 0.5) do px(x, y, c) end
    end
  end
end

-- tail fluke, left side
tri(26, 42, 40, 52, 37, 58, WHITE)
tri(26, 70, 40, 60, 37, 58, LIGHT)
rect(34, 54, 43, 60, WHITE)

-- body and belly
ellipse(55, 58, 21, 16, WHITE)
for y = 58, 74 do
  for x = 34, 76 do
    local dx, dy = (x - 55) / 21.0, (y - 58) / 16.0
    if dx * dx + dy * dy <= 1 then px(x, y, LIGHT) end
  end
end
for y = 66, 74 do
  for x = 38, 72 do
    local dx, dy = (x - 55) / 21.0, (y - 58) / 16.0
    if dx * dx + dy * dy <= 1 then px(x, y, MID) end
  end
end

-- forehead bump
ellipse(68, 49, 9, 7, WHITE)
rect(60, 42, 74, 44, WHITE)

-- beak and smile
ellipse(82, 58, 7, 3, WHITE)
ellipse(82, 60, 6, 2, LIGHT)
rect(77, 62, 84, 62, DARK)

-- eye: large, glossy, friendly
rect(66, 49, 69, 54, BLACK)
rect(67, 50, 68, 51, WHITE)
px(69, 52, CYAN)
rect(66, 47, 70, 48, LIGHT)

-- dorsal fin
tri(46, 44, 53, 43, 51, 38, WHITE)
tri(51, 38, 53, 43, 55, 44, LIGHT)

-- pectoral fin
tri(52, 70, 60, 72, 46, 82, LIGHT)
tri(46, 82, 60, 72, 55, 82, MID)

-- blowhole + cyan spout
rect(66, 40, 69, 41, MID)
px(66, 36, CYAN)
px(69, 33, CYAN)
px(72, 30, CYAN)
px(64, 33, CYAN)
px(60, 30, CYAN)

-- soft wave under the dolphin
for x = 36, 74 do
  local wave = math.floor(3 * math.sin((x - 36) / 6.0))
  px(x, 86 + wave, CYAN)
end
for x = 30, 68 do
  local wave = math.floor(2 * math.sin((x - 30) / 5.0))
  px(x, 91 + wave, MID)
end

-- crisp bottom outline so the icon reads on light and dark launchers
for x = 36, 74 do
  local yy = 58 + math.floor(16 * math.sqrt(math.max(0, 1 - (x - 55) * (x - 55) / 441.0)))
  px(x, yy, DARK)
end

spr.filename = ART .. "/px_logo.aseprite"
spr:saveAs(ART .. "/px_logo.aseprite")
app.command.ExportSpriteSheet{
  ui = false,
  type = SpriteSheetType.HORIZONTAL,
  textureFilename = RES .. "/px_logo.png",
  dataFilename = ART .. "/px_logo.json",
  dataFormat = SpriteSheetDataFormat.JSON_ARRAY,
  trim = false, borderPadding = 0, shapePadding = 0, innerPadding = 0,
}

-- Adaptive foreground is the same art with transparency, shifted into the
-- 66dp safe zone so round launcher masks never clip the dolphin or waves.
local fg = Image(W, H)
local SHIFT_X = 3 * SCALE
local SHIFT_Y = 7 * SCALE
for y = 0, H - 1 do
  for x = 0, W - 1 do
    local sx, sy = x + SHIFT_X, y + SHIFT_Y
    if sx < W and sy < H then
      local c = img:getPixel(sx, sy)
      if app.pixelColor.rgbaA(c) > 0 then fg:drawPixel(x, y, c) end
    end
  end
end
local fgSpr = Sprite(W, H, ColorMode.RGB)
fgSpr.layers[1].name = "foreground"
fgSpr:newCel(fgSpr.layers[1], 1, fg, Point(0, 0))
fgSpr.filename = ART .. "/ic_launcher_foreground.aseprite"
fgSpr:saveAs(ART .. "/ic_launcher_foreground.aseprite")
app.command.ExportSpriteSheet{
  ui = false,
  type = SpriteSheetType.HORIZONTAL,
  textureFilename = RES .. "/ic_launcher_foreground.png",
  dataFilename = ART .. "/ic_launcher_foreground.json",
  dataFormat = SpriteSheetDataFormat.JSON_ARRAY,
  trim = false, borderPadding = 0, shapePadding = 0, innerPadding = 0,
}
print("logo ok")
