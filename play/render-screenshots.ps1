Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$fontDir = Join-Path $root "..\app\src\main\res\font"
$outDir = Join-Path $root "screenshots"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$kept = New-Object System.Collections.Generic.List[object]
function Font-Of([string]$file, [float]$size) {
    $pfc = New-Object System.Drawing.Text.PrivateFontCollection
    $pfc.AddFontFile((Join-Path $fontDir $file))
    $kept.Add($pfc) | Out-Null
    return New-Object System.Drawing.Font($pfc.Families[0], $size, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
}

$reg = { param($s) Font-Of "outfit_regular.ttf" $s }
$med = { param($s) Font-Of "outfit_medium.ttf" $s }
$semi = { param($s) Font-Of "outfit_semibold.ttf" $s }
$bold = { param($s) Font-Of "outfit_bold.ttf" $s }

$ink = [System.Drawing.Color]::FromArgb(255, 8, 9, 12)
$raised = [System.Drawing.Color]::FromArgb(255, 16, 19, 26)
$card = [System.Drawing.Color]::FromArgb(255, 23, 28, 38)
$cardHigh = [System.Drawing.Color]::FromArgb(255, 33, 40, 54)
$line = [System.Drawing.Color]::FromArgb(255, 44, 52, 66)
$paper = [System.Drawing.Color]::FromArgb(255, 247, 244, 238)
$mute = [System.Drawing.Color]::FromArgb(255, 167, 174, 184)
$faint = [System.Drawing.Color]::FromArgb(255, 122, 130, 142)
$signal = [System.Drawing.Color]::FromArgb(255, 214, 242, 90)
$onSignal = [System.Drawing.Color]::FromArgb(255, 20, 22, 28)
$coral = [System.Drawing.Color]::FromArgb(255, 255, 93, 115)
$amber = [System.Drawing.Color]::FromArgb(255, 255, 183, 3)
$focus = [System.Drawing.Color]::FromArgb(255, 196, 181, 253)
$yt = [System.Drawing.Color]::FromArgb(255, 255, 59, 48)
$ig = [System.Drawing.Color]::FromArgb(255, 225, 48, 108)

function Brush([System.Drawing.Color]$c) { New-Object System.Drawing.SolidBrush $c }
function Round-Path([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = [Math]::Max(2, $r * 2)
    $p.AddArc($x, $y, $d, $d, 180, 90)
    $p.AddArc($x + $w - $d, $y, $d, $d, 270, 90)
    $p.AddArc($x + $w - $d, $y + $h - $d, $d, $d, 0, 90)
    $p.AddArc($x, $y + $h - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}
function Fill-Round($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r, [System.Drawing.Color]$c) {
    $path = Round-Path $x $y $w $h $r
    $b = Brush $c
    $g.FillPath($b, $path)
    $b.Dispose(); $path.Dispose()
}
function Stroke-Round($g, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r, [System.Drawing.Color]$c, [float]$width) {
    $path = Round-Path $x $y $w $h $r
    $pen = New-Object System.Drawing.Pen $c, $width
    $g.DrawPath($pen, $path)
    $pen.Dispose(); $path.Dispose()
}
function Text-Width($g, [string]$text, $font) {
    $size = $g.MeasureString($text, $font)
    return $size.Width
}
function Draw-Text($g, [string]$text, $font, [System.Drawing.Color]$c, [float]$x, [float]$y) {
    $b = Brush $c
    $g.DrawString($text, $font, $b, $x, $y)
    $b.Dispose()
}
function Draw-Center($g, [string]$text, $font, [System.Drawing.Color]$c, [float]$cx, [float]$y) {
    $w = Text-Width $g $text $font
    Draw-Text $g $text $font $c ($cx - $w / 2) $y
}
function Draw-Wrap($g, [string]$text, $font, [System.Drawing.Color]$c, [float]$x, [float]$y, [float]$w) {
    $b = Brush $c
    $rect = New-Object System.Drawing.RectangleF $x, $y, $w, 400
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Near
    $g.DrawString($text, $font, $b, $rect, $fmt)
    $b.Dispose(); $fmt.Dispose()
    return $g.MeasureString($text, $font, [int]$w).Height
}
function Draw-CenterWrap($g, [string]$text, $font, [System.Drawing.Color]$c, [float]$x, [float]$y, [float]$w) {
    $b = Brush $c
    $rect = New-Object System.Drawing.RectangleF $x, $y, $w, 300
    $fmt = New-Object System.Drawing.StringFormat
    $fmt.Alignment = [System.Drawing.StringAlignment]::Center
    $g.DrawString($text, $font, $b, $rect, $fmt)
    $b.Dispose(); $fmt.Dispose()
    return $g.MeasureString($text, $font, [int]$w).Height
}
function Draw-Ring($g, [float]$cx, [float]$cy, [float]$d, [float]$stroke, [float]$sweep, [System.Drawing.Color]$color) {
    $x = $cx - $d / 2
    $y = $cy - $d / 2
    $track = New-Object System.Drawing.Pen $line, $stroke
    $track.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $track.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawArc($track, $x, $y, $d, $d, 0, 360)
    $track.Dispose()
    if ($sweep -gt 1) {
        $pen = New-Object System.Drawing.Pen $color, $stroke
        $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
        $g.DrawArc($pen, $x, $y, $d, $d, -90, $sweep)
        $pen.Dispose()
    }
}
function Draw-Button($g, [float]$x, [float]$y, [float]$w, [float]$h, [string]$text, $font, [System.Drawing.Color]$fill, [System.Drawing.Color]$fore, [System.Drawing.Color]$border) {
    Fill-Round $g $x $y $w $h ($h / 2) $fill
    if ($border.A -gt 0) { Stroke-Round $g $x $y $w $h ($h / 2) $border 2.5 }
    $b = Brush $fore
    $size = $g.MeasureString($text, $font)
    $g.DrawString($text, $font, $b, ($x + ($w - $size.Width) / 2), ($y + ($h - $size.Height) / 2 - 1))
    $b.Dispose()
}
function Draw-Pill($g, [float]$x, [float]$y, [string]$text, $font, [System.Drawing.Color]$fill, [System.Drawing.Color]$fore) {
    $w = (Text-Width $g $text $font) + 36
    $h = 44
    Fill-Round $g $x $y $w $h 22 $fill
    Draw-Text $g $text $font $fore ($x + 18) ($y + 6)
    return $w
}
function Draw-Nav($g, [float]$x, [float]$y, [float]$w, [float]$h, [string]$selected) {
    $labels = @("Home", "Insights", "Focus", "Settings")
    $slot = $w / 4
    $font = & $semi 20
    for ($i = 0; $i -lt 4; $i++) {
        $on = $labels[$i] -eq $selected
        $c = if ($on) { $signal } else { $faint }
        $cx = $x + $slot * $i + $slot / 2
        $cy = $y + 22
        $pen = New-Object System.Drawing.Pen $c, 3
        $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
        $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
        switch ($labels[$i]) {
            "Home" {
                $g.DrawRectangle($pen, $cx - 11, $cy - 4, 22, 16)
                $g.DrawLine($pen, $cx - 14, $cy - 2, $cx, $cy - 14)
                $g.DrawLine($pen, $cx, $cy - 14, $cx + 14, $cy - 2)
            }
            "Insights" {
                $g.DrawLine($pen, $cx - 12, $cy + 10, $cx - 12, $cy - 2)
                $g.DrawLine($pen, $cx, $cy + 10, $cx, $cy - 10)
                $g.DrawLine($pen, $cx + 12, $cy + 10, $cx + 12, $cy + 2)
            }
            "Focus" {
                $g.DrawEllipse($pen, $cx - 12, $cy - 12, 24, 24)
                $g.DrawLine($pen, $cx, $cy, $cx, $cy - 7)
                $g.DrawLine($pen, $cx, $cy, $cx + 6, $cy + 1)
            }
            "Settings" {
                $g.DrawEllipse($pen, $cx - 5, $cy - 5, 10, 10)
                for ($k = 0; $k -lt 8; $k++) {
                    $a = $k * [Math]::PI / 4
                    $g.DrawLine($pen, $cx + [Math]::Cos($a) * 9, $cy + [Math]::Sin($a) * 9, $cx + [Math]::Cos($a) * 14, $cy + [Math]::Sin($a) * 14)
                }
            }
        }
        $pen.Dispose()
        Draw-Center $g $labels[$i] $font $c $cx ($y + 46)
    }
}
function Draw-AppIcon($g, [float]$x, [float]$y, [float]$s, [string]$kind) {
    switch ($kind) {
        "yt" {
            Fill-Round $g $x $y $s $s ($s * 0.28) $yt
            $b = Brush ([System.Drawing.Color]::White)
            $path = New-Object System.Drawing.Drawing2D.GraphicsPath
            $path.AddPolygon(@(
                (New-Object System.Drawing.PointF ($x + $s * 0.38), ($y + $s * 0.28)),
                (New-Object System.Drawing.PointF ($x + $s * 0.38), ($y + $s * 0.72)),
                (New-Object System.Drawing.PointF ($x + $s * 0.74), ($y + $s * 0.50))
            ))
            $g.FillPath($b, $path)
            $b.Dispose(); $path.Dispose()
        }
        "ig" {
            Fill-Round $g $x $y $s $s ($s * 0.28) $ig
            $pen = New-Object System.Drawing.Pen ([System.Drawing.Color]::White), ($s * 0.07)
            $g.DrawEllipse($pen, $x + $s * 0.28, $y + $s * 0.28, $s * 0.44, $s * 0.44)
            $g.FillEllipse((Brush ([System.Drawing.Color]::White)), $x + $s * 0.70, $y + $s * 0.22, $s * 0.08, $s * 0.08)
            $pen.Dispose()
        }
        "tt" {
            Fill-Round $g $x $y $s $s ($s * 0.28) ([System.Drawing.Color]::FromArgb(255, 20, 20, 20))
            Stroke-Round $g $x $y $s $s ($s * 0.28) $line 2
            Draw-Center $g "TT" (& $bold ($s * 0.32)) $paper ($x + $s / 2) ($y + $s * 0.28)
        }
        "game" {
            Fill-Round $g $x $y $s $s ($s * 0.28) $signal
            Draw-Center $g "G" (& $bold ($s * 0.42)) $onSignal ($x + $s / 2) ($y + $s * 0.22)
        }
    }
}
function Draw-Status($g, [float]$x, [float]$w, [float]$y) {
    Draw-Text $g "9:41" (& $semi 22) $paper ($x + 36) $y
    $pen = New-Object System.Drawing.Pen $paper, 2
    $bx = $x + $w - 78
    $g.DrawRectangle($pen, $bx, $y + 6, 28, 14)
    $g.FillRectangle((Brush $paper), $bx + 2, $y + 8, 18, 10)
    $pen.Dispose()
}

function New-Canvas {
    $bmp = New-Object System.Drawing.Bitmap 1080, 1920, ([System.Drawing.Imaging.PixelFormat]::Format24bppRgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.Clear($ink)
    return @{ Bmp = $bmp; G = $g }
}
function Draw-Header($g, [string]$title, [string]$sub) {
    $mark = 36
    $pen = New-Object System.Drawing.Pen $signal, 5
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $g.DrawArc($pen, 72, 78, $mark, $mark, 116, 292)
    $pen.Dispose()
    Draw-Text $g "ScrnLimit" (& $semi 28) $paper 120 82
    Draw-Text $g $title (& $bold 78) $paper 72 150
    $h = Draw-Wrap $g $sub (& $reg 30) $mute 72 250 936
    return 250 + $h
}
function Begin-Phone($g, [float]$top) {
    $x = 78; $y = $top; $w = 924; $h = 1920 - $top - 48
    Fill-Round $g ($x - 10) ($y + 18) $w $h 58 ([System.Drawing.Color]::FromArgb(255, 0, 0, 0))
    Fill-Round $g $x $y $w $h 52 $raised
    $sx = $x + 16; $sy = $y + 16; $sw = $w - 32; $sh = $h - 32
    $path = Round-Path $sx $sy $sw $sh 40
    $g.SetClip($path)
    $g.FillPath((Brush $ink), $path)
    $path.Dispose()
    return @{ X = $sx; Y = $sy; W = $sw; H = $sh }
}
function End-Phone($g) { $g.ResetClip() }

function Screen-Home($g, $p) {
    $raw = [System.Drawing.Bitmap]::FromFile((Join-Path $root "raw\home.png"))
    $clean = New-Object System.Drawing.Bitmap $raw.Width, $raw.Height, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $cg = [System.Drawing.Graphics]::FromImage($clean)
    $cg.DrawImage($raw, 0, 0, $raw.Width, $raw.Height)
    $shift = 648
    $srcX = 700; $srcY = 1640; $srcW = 330; $srcH = 230
    $destRect = New-Object System.Drawing.Rectangle ($srcX - $shift), $srcY, $srcW, $srcH
    $srcRect = New-Object System.Drawing.Rectangle $srcX, $srcY, $srcW, $srcH
    $cg.DrawImage($raw, $destRect, $srcRect, [System.Drawing.GraphicsUnit]::Pixel)
    $cg.Dispose()
    $raw.Dispose()

    $inkPx = [System.Drawing.Color]::FromArgb(255, 8, 9, 12)
    $cardPx = [System.Drawing.Color]::FromArgb(255, 23, 28, 38)
    for ($y = 1660; $y -le 1888; $y++) {
        for ($x = 0; $x -le 64; $x++) { $clean.SetPixel($x, $y, $inkPx) }
    }
    for ($y = 1862; $y -le 1900; $y++) {
        for ($x = 0; $x -le 400; $x++) { $clean.SetPixel($x, $y, $inkPx) }
    }
    for ($y = 1688; $y -le 1828; $y++) {
        for ($x = 96; $x -le 260; $x++) {
            $c = $clean.GetPixel($x, $y)
            if ($c.R -gt 70 -and $c.G -gt 70 -and $c.B -gt 70) { $clean.SetPixel($x, $y, $cardPx) }
        }
    }

    $tg = [System.Drawing.Graphics]::FromImage($clean)
    $tg.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $labelFont = & $med 29
    $valueFont = & $bold 42
    $fmt = [System.Drawing.StringFormat]::GenericTypographic
    $lb = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 122, 130, 142))
    $vb = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 247, 244, 238))
    $tg.DrawString("Opens stopped", $labelFont, $lb, 104, 1698, $fmt)
    $tg.DrawString("0", $valueFont, $vb, 104, 1758, $fmt)
    $lb.Dispose(); $vb.Dispose(); $tg.Dispose()

    $srcY0 = 118
    $srcH0 = 1740
    $scale = $p.W / 1080.0
    $drawH = $srcH0 * $scale
    $dy = $p.Y + [Math]::Max(8, ($p.H - $drawH) / 2)
    $dest = New-Object System.Drawing.RectangleF ([single]$p.X), ([single]$dy), ([single]$p.W), ([single]$drawH)
    $src = New-Object System.Drawing.RectangleF 0, $srcY0, 1080, $srcH0
    $prev = $g.InterpolationMode
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.DrawImage($clean, $dest, $src, [System.Drawing.GraphicsUnit]::Pixel)
    $g.InterpolationMode = $prev
    $clean.Dispose()
}
function Screen-Pause($g, $p) {
    Draw-Status $g $p.X $p.W ($p.Y + 28)
    $cx = $p.X + $p.W / 2
    Draw-AppIcon $g ($cx - 36) ($p.Y + 120) 72 "yt"
    Draw-Center $g "YouTube" (& $med 26) $mute $cx ($p.Y + 208)
    Draw-Ring $g $cx ($p.Y + 430) 280 26 300 $signal
    Draw-Center $g "8" (& $bold 92) $paper $cx ($p.Y + 370)
    Draw-CenterWrap $g "Is this important?" (& $semi 44) $paper ($p.X + 48) ($p.Y + 620) ($p.W - 96)
    Draw-Center $g "Breathe in" (& $reg 26) $mute $cx ($p.Y + 700)
    Draw-Button $g ($p.X + 40) ($p.Y + $p.H - 280) ($p.W - 80) 84 "Not now" (& $semi 30) $signal $onSignal ([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
    Draw-Button $g ($p.X + 40) ($p.Y + $p.H - 176) ($p.W - 80) 84 "Open for 5 min" (& $semi 30) $ink $paper $line
}
function Screen-Cap($g, $p) {
    Draw-Status $g $p.X $p.W ($p.Y + 28)
    $cx = $p.X + $p.W / 2
    Draw-AppIcon $g ($cx - 36) ($p.Y + 110) 72 "yt"
    Draw-Center $g "YouTube" (& $med 26) $mute $cx ($p.Y + 198)
    Draw-Ring $g $cx ($p.Y + 430) 280 26 360 $coral
    Draw-Center $g "Stop" (& $bold 64) $paper $cx ($p.Y + 390)
    Draw-CenterWrap $g "You've hit the cap" (& $semi 42) $paper ($p.X + 40) ($p.Y + 610) ($p.W - 80)
    $bodyH = Draw-CenterWrap $g "The distracting set is at its daily limit." (& $reg 24) $mute ($p.X + 70) ($p.Y + 690) ($p.W - 140)
    Draw-Button $g ($p.X + 40) ($p.Y + $p.H - 300) ($p.W - 80) 84 "Close" (& $semi 30) $signal $onSignal ([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
    Draw-Button $g ($p.X + 40) ($p.Y + $p.H - 196) ($p.W - 80) 84 "Use 1 emergency pass" (& $semi 28) $ink $coral $coral
    Draw-Center $g "3 left today" (& $med 22) $faint $cx ($p.Y + $p.H - 96)
}
function Screen-Reels($g, $p) {
    $g.FillRectangle((Brush ([System.Drawing.Color]::FromArgb(255, 18, 16, 22))), $p.X, $p.Y, $p.W, $p.H)
    $ghost = [System.Drawing.Color]::FromArgb(255, 70, 74, 82)
    $rx = $p.X + $p.W - 92
    $ry = $p.Y + 280
    for ($i = 0; $i -lt 4; $i++) {
        $g.DrawEllipse((New-Object System.Drawing.Pen $ghost, 3), $rx, $ry, 48, 48)
        $ry += 92
    }
    $dim = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(185, 8, 9, 12))
    $g.FillRectangle($dim, $p.X, $p.Y, $p.W, $p.H)
    $dim.Dispose()
    $cx = $p.X + $p.W / 2
    Draw-Center $g "IN THE APP" (& $semi 22) $signal $cx ($p.Y + 430)
    Draw-CenterWrap $g "Reels stay blocked" (& $bold 52) $paper ($p.X + 50) ($p.Y + 480) ($p.W - 100)
    Draw-CenterWrap $g "The rest of the app can stay open. This feed does not." (& $reg 26) $mute ($p.X + 70) ($p.Y + 640) ($p.W - 140)
    Draw-Button $g ($p.X + 56) ($p.Y + 820) ($p.W - 112) 84 "Go back" (& $semi 30) $signal $onSignal ([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
}
function Screen-Focus($g, $p) {
    Draw-Status $g $p.X $p.W ($p.Y + 28)
    Draw-Text $g "FOCUS" (& $semi 22) $focus ($p.X + 40) ($p.Y + 96)
    Draw-Text $g "Block the apps that steal time." (& $bold 40) $paper ($p.X + 40) ($p.Y + 146)
    Draw-Wrap $g "The list is already chosen. A streak is a session you finish." (& $reg 24) $mute ($p.X + 40) ($p.Y + 250) ($p.W - 80)
    $chips = @(@("25 min", $true), @("50 min", $false), @("90 min", $false))
    $cx0 = $p.X + 40
    $cy = $p.Y + 360
    foreach ($chip in $chips) {
        $font = & $semi 24
        $tw = (Text-Width $g $chip[0] $font) + 40
        if ($chip[1]) { Fill-Round $g $cx0 $cy $tw 56 28 $focus }
        else { Stroke-Round $g $cx0 $cy $tw 56 28 $line 2 }
        $fc = if ($chip[1]) { $onSignal } else { $paper }
        Draw-Text $g $chip[0] $font $fc ($cx0 + 20) ($cy + 12)
        $cx0 += $tw + 14
    }
    Draw-Text $g "Pomodoro" (& $semi 28) $paper ($p.X + 40) ($p.Y + 470)
    Draw-Text $g "25 on, 5 off" (& $reg 22) $mute ($p.X + 40) ($p.Y + 514)
    Fill-Round $g ($p.X + $p.W - 130) ($p.Y + 476) 70 40 20 $focus
    $g.FillEllipse((Brush $onSignal), ($p.X + $p.W - 96), ($p.Y + 482), 28, 28)
    Draw-Center $g "25:00" (& $bold 96) $paper ($p.X + $p.W / 2) ($p.Y + 640)
    Draw-Center $g "ready when you are" (& $med 22) $faint ($p.X + $p.W / 2) ($p.Y + 760)
    Draw-Button $g ($p.X + 40) ($p.Y + 860) ($p.W - 80) 84 "Focus for 25 min" (& $semi 30) $focus $onSignal ([System.Drawing.Color]::FromArgb(0, 0, 0, 0))
    Draw-Nav $g $p.X ($p.Y + $p.H - 96) $p.W 96 "Focus"
}
function Screen-Insights($g, $p) {
    Draw-Status $g $p.X $p.W ($p.Y + 28)
    Draw-Text $g "THIS WEEK" (& $semi 22) $signal ($p.X + 40) ($p.Y + 96)
    Draw-Text $g "4h 12m" (& $bold 72) $paper ($p.X + 40) ($p.Y + 140)
    Draw-Text $g "distracting, 38m under last week" (& $med 22) $mute ($p.X + 40) ($p.Y + 230)
    $bars = @(0.32, 0.48, 0.4, 0.62, 0.55, 0.78, 1.0)
    $days = @("M", "T", "W", "T", "F", "S", "S")
    $base = $p.Y + 520
    $left = $p.X + 48
    $bw = 64
    $gap = 28
    $maxH = 220
    for ($i = 0; $i -lt 7; $i++) {
        $bh = $maxH * $bars[$i]
        $bx = $left + $i * ($bw + $gap)
        $by = $base - $bh
        $c = if ($i -eq 6) { $signal } else { [System.Drawing.Color]::FromArgb(255, 122, 148, 48) }
        Fill-Round $g $bx $by $bw $bh 12 $c
        Draw-Center $g $days[$i] (& $med 18) $faint ($bx + $bw / 2) ($base + 12)
    }
    Fill-Round $g ($p.X + 40) ($p.Y + 600) ($p.W - 80) 150 24 $card
    Draw-Text $g "Peak  9 PM" (& $semi 26) $paper ($p.X + 64) ($p.Y + 624)
    $hx = $p.X + 64
    $hy = $p.Y + 690
    for ($h = 0; $h -lt 24; $h++) {
        $frac = 0.15 + 0.7 * [Math]::Exp(-[Math]::Pow(($h - 21) / 4.2, 2))
        $hh = 8 + 46 * $frac
        $col = if ($h -eq 21) { $amber } else { [System.Drawing.Color]::FromArgb(140, 214, 242, 90) }
        Fill-Round $g $hx ($hy + 46 - $hh) 16 $hh 4 $col
        $hx += 28
    }
    Draw-Nav $g $p.X ($p.Y + $p.H - 96) $p.W 96 "Insights"
}
function Screen-Rules($g, $p) {
    Draw-Status $g $p.X $p.W ($p.Y + 28)
    Draw-Text $g "THE CAP" (& $semi 22) $signal ($p.X + 40) ($p.Y + 96)
    Draw-Text $g "1 hour" (& $bold 64) $paper ($p.X + 40) ($p.Y + 140)
    Draw-Text $g "Shared by every distracting app" (& $reg 24) $mute ($p.X + 40) ($p.Y + 220)
    $rows = @(
        @("yt", "YouTube", "38m today"),
        @("ig", "Instagram", "4m today"),
        @("tt", "TikTok", "Not yet today"),
        @("game", "Games", "Included in the hour")
    )
    $y = $p.Y + 300
    foreach ($row in $rows) {
        Fill-Round $g ($p.X + 36) $y ($p.W - 72) 108 24 $card
        Draw-AppIcon $g ($p.X + 56) ($y + 22) 64 $row[0]
        Draw-Text $g $row[1] (& $semi 28) $paper ($p.X + 140) ($y + 22)
        Draw-Text $g $row[2] (& $reg 22) $mute ($p.X + 140) ($y + 58)
        $y += 122
    }
    Fill-Round $g ($p.X + 36) $y ($p.W - 72) 108 24 $card
    Draw-Text $g "Block Reels and Shorts" (& $semi 26) $paper ($p.X + 60) ($y + 22)
    Draw-Text $g "Instagram, YouTube, Facebook" (& $reg 20) $mute ($p.X + 60) ($y + 58)
    Fill-Round $g ($p.X + $p.W - 150) ($y + 34) 78 40 20 $signal
    $g.FillEllipse((Brush $onSignal), ($p.X + $p.W - 118), ($y + 40), 28, 28)
    Draw-Nav $g $p.X ($p.Y + $p.H - 96) $p.W 96 "Home"
}

$shots = @(
    @{ File = "01-one-daily-limit.png"; Title = "One daily limit"; Sub = "YouTube, Instagram, TikTok, and games share one hour."; Draw = { param($g, $p) Screen-Home $g $p } },
    @{ File = "02-pause.png"; Title = "Pause before you open"; Sub = "Eight seconds to ask if this is important, then a short burst."; Draw = { param($g, $p) Screen-Pause $g $p } },
    @{ File = "03-at-the-cap.png"; Title = "Then it stops"; Sub = "At the cap, distracting apps stay covered until tomorrow."; Draw = { param($g, $p) Screen-Cap $g $p } },
    @{ File = "04-reels.png"; Title = "Block the feed"; Sub = "Reels and Shorts stop. The rest of the app can stay open."; Draw = { param($g, $p) Screen-Reels $g $p } },
    @{ File = "05-focus.png"; Title = "A clear stretch"; Sub = "25, 50, or 90 minutes, or a Pomodoro of 25 on and 5 off."; Draw = { param($g, $p) Screen-Focus $g $p } },
    @{ File = "06-insights.png"; Title = "See the hours"; Sub = "Today against yesterday, the last 7 days, and peak hours."; Draw = { param($g, $p) Screen-Insights $g $p } },
    @{ File = "07-your-apps.png"; Title = "Your apps, your hour"; Sub = "One shared cap, and Reels and Shorts blocked inside the app."; Draw = { param($g, $p) Screen-Rules $g $p } }
)

foreach ($shot in $shots) {
    if ($env:SL_SHOT -and $shot.File -ne $env:SL_SHOT) { continue }
    $canvas = New-Canvas
    $g = $canvas.G
    $bottom = Draw-Header $g $shot.Title $shot.Sub
    $phone = Begin-Phone $g ($bottom + 36)
    & $shot.Draw $g $phone
    End-Phone $g
    $path = Join-Path $outDir $shot.File
    $canvas.Bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $canvas.Bmp.Dispose()
    $item = Get-Item $path
    Write-Output ("{0}  {1}x  {2} bytes" -f $item.Name, "1080x1920", $item.Length)
}
