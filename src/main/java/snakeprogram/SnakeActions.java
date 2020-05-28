package snakeprogram;

/**
 * For delegating actions from the gui to the mdoel. This is an oudated pattern, but it is deeply engrained in this
 * program.
 *
 * Created on 05/01/2017.
 */
public enum SnakeActions {
    previousImage,
    nextImage,
    getAndLoad,
    setAlpha,
    addSnake,
    deformSnake,
    setBeta,
    setGamma,
    setWeight,
    getForeground,
    setForeground,
    setStretch,
    stretchFix,
    getBackground,
    setBackground,
    setIterations,
    saveSnakes,
    loadSnakes,
    deleteSnake,
    initializeZoom,
    zoomOut,
    deleteEnd,
    deleteMiddle,
    trackSnake,
    saveData,
    setResolution,
    setSigma,
    deformFix,
    setMaxLength,
    setLineWidth,
    showVersion,
    trackAllFrames,
    deformAllFrames,
    trackBackwards,
    guessForeBackground,
    repositionEnd,
    moveAndRotate,
    fission,
    sculpt, setBalloonForce;
}
