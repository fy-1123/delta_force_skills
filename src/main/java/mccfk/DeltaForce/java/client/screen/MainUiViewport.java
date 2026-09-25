package mccfk.DeltaForce.java.client.screen;

/** Matches the fixed reference-coordinate model used by the settings screen. */
public record MainUiViewport(int canvasWidth, int canvasHeight, float scale, float offsetX, float offsetY) {
    public static final int WIDTH = 960;
    public static final int HEIGHT = 540;

    public static MainUiViewport fit(int guiWidth, int guiHeight) {
        int safeWidth = Math.max(1, guiWidth);
        int safeHeight = Math.max(1, guiHeight);
        float scale = Math.min(safeWidth / (float) WIDTH, safeHeight / (float) HEIGHT);
        return new MainUiViewport(WIDTH, HEIGHT, scale,
                (safeWidth - WIDTH * scale) / 2.0F,
                (safeHeight - HEIGHT * scale) / 2.0F);
    }

    public double toCanvasX(double x) {
        return (x - offsetX) / scale;
    }

    public double toCanvasY(double y) {
        return (y - offsetY) / scale;
    }

    public boolean contains(double x, double y) {
        double canvasX = toCanvasX(x);
        double canvasY = toCanvasY(y);
        return canvasX >= 0 && canvasX < canvasWidth && canvasY >= 0 && canvasY < canvasHeight;
    }
}