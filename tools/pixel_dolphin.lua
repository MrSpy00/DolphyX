local ROOT = app.fs.currentPath
local RES = ROOT .. "/app/src/main/res/drawable-nodpi"
local ART = ROOT .. "/tools/art"
app.fs.makeDirectory(RES)
app.fs.makeDirectory(ART)

local W, H = 96, 96
local spr = Sprite(W, H, ColorMode.RGB)
spr.layers[1].name = "dolphin"
local cel = spr:newCel(spr.layers[1], 1, Image(W, H), Point(0, 0))
local img = cel.image

local function px(x, y, c)
  if x >= 0 and y >= 0 and x < W and y < H then img:drawPixel(x, y, c) end
end

local function col(v, a)
  return app.pixelColor.rgba(v, v, v, a or 255)
end

local WHITE = col(238)
local LIGHT = col(196)
local MID = col(132)
local DARK = col(54)
local BLACK = col(8)

local function rect(x0, y0, x1, y1, c)
  for y = y0, y1 do
    for x = x0, x1 do px(x, y, c) end
  end
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

-- tail fluke (left)
tri(8, 36, 26, 48, 24, 52, WHITE)
tri(8, 62, 26, 56, 24, 52, LIGHT)
rect(18, 48, 28, 56, WHITE)

-- body
ellipse(48, 52, 30, 18, WHITE)
for y = 52, 70 do
  for x = 20, 76 do
    local dx, dy = (x - 48) / 30.0, (y - 52) / 18.0
    if dx * dx + dy * dy <= 1 then px(x, y, LIGHT) end
  end
end
for y = 60, 70 do
  for x = 24, 72 do
    local dx, dy = (x - 48) / 30.0, (y - 52) / 18.0
    if dx * dx + dy * dy <= 1 then px(x, y, MID) end
  end
end

-- melon / forehead bump
ellipse(66, 42, 12, 9, WHITE)
rect(56, 34, 72, 36, WHITE)

-- beak
tri(72, 48, 94, 54, 72, 56, WHITE)
tri(72, 56, 92, 56, 72, 60, LIGHT)
rect(72, 57, 90, 58, DARK)
px(70, 59, DARK)
px(69, 58, DARK)
rect(74, 59, 86, 60, LIGHT)

-- eye
rect(62, 40, 67, 46, BLACK)
rect(63, 41, 64, 42, WHITE)
rect(60, 37, 69, 38, LIGHT)

-- dorsal fin
tri(34, 35, 46, 34, 42, 22, WHITE)
tri(42, 22, 46, 34, 50, 36, LIGHT)
rect(38, 32, 41, 34, MID)

-- pectoral fin
tri(48, 62, 56, 64, 40, 78, LIGHT)
tri(40, 78, 56, 64, 50, 78, MID)

-- blowhole
rect(64, 33, 68, 34, MID)

-- belly outline
for x = 20, 74 do
  local yy = 52 + math.floor(18 * math.sqrt(math.max(0, 1 - (x - 48) * (x - 48) / 900.0)))
  px(x, yy, DARK)
end

spr.filename = ART .. "/px_dolphin.aseprite"
spr:saveAs(ART .. "/px_dolphin.aseprite")
app.command.ExportSpriteSheet{
  ui = false,
  type = SpriteSheetType.HORIZONTAL,
  textureFilename = RES .. "/px_dolphin.png",
  dataFilename = ART .. "/px_dolphin.json",
  dataFormat = SpriteSheetDataFormat.JSON_ARRAY,
  trim = false, borderPadding = 0, shapePadding = 0, innerPadding = 0,
}
print("dolphin ok")
