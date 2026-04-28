Add-Type -AssemblyName System.Drawing

$outDir = Join-Path $PSScriptRoot "..\\graphics\\ui"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

$width = 30
$height = 18

function New-BadgePng {
    param(
        [string]$Path,
        [string]$Text,
        [System.Drawing.Color]$Background,
        [System.Drawing.Color]$Foreground
    )

    $bmp = New-Object System.Drawing.Bitmap($width, $height, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $borderPen = $null
    $font = $null
    $format = $null
    $brush = $null
    try {
        $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
        $g.Clear($Background)
        $borderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::Black, 1)
        $g.DrawRectangle($borderPen, 0, 0, $width - 1, $height - 1)

        $font = New-Object System.Drawing.Font("Arial", 11, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
        $format = New-Object System.Drawing.StringFormat
        $format.Alignment = [System.Drawing.StringAlignment]::Center
        $format.LineAlignment = [System.Drawing.StringAlignment]::Center
        $brush = New-Object System.Drawing.SolidBrush($Foreground)
        $rect = New-Object System.Drawing.RectangleF(0, 0, $width, $height)
        $g.DrawString($Text, $font, $brush, $rect, $format)

        $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        if ($brush) { $brush.Dispose() }
        if ($format) { $format.Dispose() }
        if ($font) { $font.Dispose() }
        if ($borderPen) { $borderPen.Dispose() }
        $g.Dispose()
        $bmp.Dispose()
    }
}

$red = [System.Drawing.ColorTranslator]::FromHtml("#F25A5A")
$yellow = [System.Drawing.ColorTranslator]::FromHtml("#FFD83D")
$green = [System.Drawing.ColorTranslator]::FromHtml("#4BCB63")
$white = [System.Drawing.Color]::White
$black = [System.Drawing.Color]::Black

New-BadgePng -Path (Join-Path $outDir "wim_total_red_0.png") -Text "0" -Background $red -Foreground $black

for ($i = 1; $i -le 9; $i++) {
    New-BadgePng -Path (Join-Path $outDir ("wim_total_yellow_{0}.png" -f $i)) -Text ([string]$i) -Background $yellow -Foreground $black
}

for ($i = 10; $i -le 98; $i++) {
    New-BadgePng -Path (Join-Path $outDir ("wim_total_green_{0}.png" -f $i)) -Text ([string]$i) -Background $green -Foreground $black
}

New-BadgePng -Path (Join-Path $outDir "wim_total_green_99plus.png") -Text "99+" -Background $green -Foreground $black
New-BadgePng -Path (Join-Path $outDir "wim_total_err.png") -Text "E" -Background $red -Foreground $black

Write-Host "Generated total badge sprites in $outDir"
