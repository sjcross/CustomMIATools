package wbif.sjx.AddFromPositionMeasurement;

import java.awt.Color;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.drew.lang.annotations.Nullable;

import ij.ImagePlus;
import ij.Prefs;
import ij.gui.OvalRoi;
import ij.gui.PointRoi;
import ij.plugin.Duplicator;
import ij.plugin.HyperStackConverter;
import wbif.sjx.MIA.MIA;
import wbif.sjx.MIA.Module.ModuleCollection;
import wbif.sjx.MIA.Module.PackageNames;
import wbif.sjx.MIA.Module.Visualisation.Overlays.AddObjectCentroid;
import wbif.sjx.MIA.Module.Visualisation.Overlays.Overlay;
import wbif.sjx.MIA.Object.Image;
import wbif.sjx.MIA.Object.Obj;
import wbif.sjx.MIA.Object.ObjCollection;
import wbif.sjx.MIA.Object.Workspace;
import wbif.sjx.MIA.Object.Parameters.BooleanP;
import wbif.sjx.MIA.Object.Parameters.ChoiceP;
import wbif.sjx.MIA.Object.Parameters.DoubleP;
import wbif.sjx.MIA.Object.Parameters.InputImageP;
import wbif.sjx.MIA.Object.Parameters.InputObjectsP;
import wbif.sjx.MIA.Object.Parameters.ObjectMeasurementP;
import wbif.sjx.MIA.Object.Parameters.OutputImageP;
import wbif.sjx.MIA.Object.Parameters.ParamSeparatorP;
import wbif.sjx.MIA.Object.Parameters.ParameterCollection;
import wbif.sjx.MIA.Object.References.ImageMeasurementRefCollection;
import wbif.sjx.MIA.Object.References.MetadataRefCollection;
import wbif.sjx.MIA.Object.References.ObjMeasurementRefCollection;

public class AddFromPositionMeasurement_0_11_5 extends Overlay {
    public static void main(String[] args) throws Exception {
        MIA.addPluginPackageName(AddFromPositionMeasurement_0_11_5.class.getCanonicalName());
        MIA.main(new String[]{});

    }

    public static final String INPUT_SEPARATOR = "Image and object input";
    public static final String INPUT_IMAGE = "Input image";
    public static final String INPUT_OBJECTS = "Input objects";

    public static final String OUTPUT_SEPARATOR = "Image output";
    public static final String APPLY_TO_INPUT = "Apply to input image";
    public static final String ADD_OUTPUT_TO_WORKSPACE = "Add output image to workspace";
    public static final String OUTPUT_IMAGE = "Output image";

    public static final String POSITION_SEPARATOR = "Overlay position";
    public static final String X_POSITION_MEASUREMENT = "X-position measurement";
    public static final String Y_POSITION_MEASUREMENT = "Y-position measurement";
    public static final String Z_POSITION_MEASUREMENT = "Z-position measurement";
    public static final String USE_RADIUS = "Use radius measurement";
    public static final String MEASUREMENT_FOR_RADIUS = "Measurement for radius";

    public static final String RENDERING_SEPARATOR = "Overlay rendering";
    public static final String LINE_WIDTH = "Line width";
    public static final String POINT_SIZE = "Point size";
    public static final String POINT_TYPE = "Point type";
    public static final String OPACITY = "Opacity (%)";
    public static final String RENDER_IN_ALL_FRAMES = "Render in all frames";
    
    public static final String EXECUTION_SEPARATOR = "Execution controls";
    public static final String ENABLE_MULTITHREADING = "Enable multithreading";

    public interface PointSizes extends AddObjectCentroid.PointSizes {}

    public interface PointTypes extends AddObjectCentroid.PointTypes {}

        public AddFromPositionMeasurement_0_11_5(ModuleCollection modules) {
        super("Add from position measurement (v0.11.5)", modules);
    }

    // METHODS FROM OTHER CLASSES IN V0.14.13 (UNAVAILABLE HERE)

    static int getSize(String size) {
        switch (size) {
            case PointSizes.TINY:
                return 0;
            case PointSizes.SMALL:
            default:
                return 1;
            case PointSizes.MEDIUM:
                return 2;
            case PointSizes.LARGE:
                return 3;
            case PointSizes.EXTRA_LARGE:
                return 4;
        }
    }

    static int getType(String type) {
        switch (type) {
            case PointTypes.HYBRID:
                return 0;
            case PointTypes.CROSS:
                return 1;
            case PointTypes.DOT:
                return 2;
            case PointTypes.CIRCLE:
            default:
                return 3;
        }
    }

    public static Color getColour(float hue, double opacity) {
        Color color;

        // If the hue was assigned as -1 (for example, no parent found), setting the
        // colour to white
        if (hue == Float.MAX_VALUE || hue == -1) {
            color = Color.getHSBColor(0f, 0f, 1f);
        } else if (hue == Float.MIN_VALUE) {
            color = Color.getHSBColor(0f, 0f, 0f);
        } else {
            // Have to addRef 1E-8 to prevent 0 values having a rounding error that makes
            // them negative
            color = Color.getHSBColor(hue + 1E-8f, 1f, 1f);
        }

        int alpha = (int) Math.round(opacity * 2.55);

        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

    }

    // END EXTRA METHODS

    public static void addOverlay(Obj object, ImagePlus ipl, Color colour, String size, String type, double lineWidth, String[] posMeasurements, @Nullable String radiusMeasurement, boolean renderInAllFrames) {
        if (ipl.getOverlay() == null) ipl.setOverlay(new ij.gui.Overlay());

        double xMean = object.getMeasurement(posMeasurements[0]).getValue();
        double yMean = object.getMeasurement(posMeasurements[1]).getValue();
        double zMean = object.getMeasurement(posMeasurements[2]).getValue();

        // Getting coordinates and settings for plotting
        int z = (int) Math.round(zMean+1);
        int t = object.getT()+1;
        int sizeVal = getSize(size);
        int typeVal = getType(type);

        if (renderInAllFrames) t = 0;
        if (radiusMeasurement == null) {
            PointRoi pointRoi = new PointRoi(xMean+0.5,yMean+0.5);
            pointRoi.setPointType(typeVal);
            pointRoi.setSize(sizeVal);
            if (ipl.isHyperStack()) {
                pointRoi.setPosition(1, z, t);
            } else {
                int pos = Math.max(Math.max(1,z),t);
                pointRoi.setPosition(pos);
            }
            pointRoi.setStrokeColor(colour);
            ipl.getOverlay().addElement(pointRoi);

        } else {
            double r = object.getMeasurement(radiusMeasurement).getValue();
            OvalRoi ovalRoi = new OvalRoi(xMean + 0.5 - r, yMean + 0.5 - r, 2 * r, 2 * r);
            if (ipl.isHyperStack()) {
                ovalRoi.setPosition(1, z, t);
            } else {
                int pos = Math.max(Math.max(1, z), t);
                ovalRoi.setPosition(pos);
            }
            ovalRoi.setStrokeColor(colour);
            ovalRoi.setStrokeWidth(lineWidth);
            ipl.getOverlay().addElement(ovalRoi);
        }
    }


    @Override
    public String getPackageName() {
        return PackageNames.VISUALISATION_OVERLAYS;
    }

    @Override
    public String getDescription() {
        return "\"AddPositionFromMeasurement\" module from v0.14.13, targetting v0.11.5.<br><br>This version has improved plotting options (line widths and markers) than the default version in v0.11.5.";

    }

    @Override
    protected boolean process(Workspace workspace) {
        // Getting parameters
        boolean applyToInput = parameters.getValue(APPLY_TO_INPUT);
        boolean addOutputToWorkspace = parameters.getValue(ADD_OUTPUT_TO_WORKSPACE);
        String outputImageName = parameters.getValue(OUTPUT_IMAGE);

        // Getting input objects
        String inputObjectsName = parameters.getValue(INPUT_OBJECTS);
        ObjCollection inputObjects = workspace.getObjects().get(inputObjectsName);

        // Getting input image
        String inputImageName = parameters.getValue(INPUT_IMAGE);
        Image inputImage = workspace.getImages().get(inputImageName);
        ImagePlus ipl = inputImage.getImagePlus();

        String xPosMeas = parameters.getValue(X_POSITION_MEASUREMENT);
        String yPosMeas = parameters.getValue(Y_POSITION_MEASUREMENT);
        String zPosMeas = parameters.getValue(Z_POSITION_MEASUREMENT);
        boolean useRadius = parameters.getValue(USE_RADIUS);
        String tempRadiusMeasurement = parameters.getValue(MEASUREMENT_FOR_RADIUS);

        double opacity = parameters.getValue(OPACITY);        
        String pointSize = parameters.getValue(POINT_SIZE);
        String pointType = parameters.getValue(POINT_TYPE);
        double lineWidth = parameters.getValue(LINE_WIDTH);
        boolean renderInAllFrames = parameters.getValue(RENDER_IN_ALL_FRAMES);
        boolean multithread = parameters.getValue(ENABLE_MULTITHREADING);

        // Only add output to workspace if not applying to input
        if (applyToInput) addOutputToWorkspace = false;

        // Duplicating the image, so the original isn't altered
        if (!applyToInput) ipl = new Duplicator().run(ipl);

        // Setting position measurements
        final String radiusMeasurement = useRadius ? tempRadiusMeasurement : null;
        String[] posMeasurements = new String[]{xPosMeas, yPosMeas, zPosMeas};

        // Generating colours for each object
        HashMap<Integer,Float> hues = getHues(inputObjects);

        // If necessary, turning the image into a HyperStack (if 2 dimensions=1 it will be a standard ImagePlus)
        if (!ipl.isComposite() & (ipl.getNSlices() > 1 | ipl.getNFrames() > 1 | ipl.getNChannels() > 1)) {
            ipl = HyperStackConverter.toHyperStack(ipl, ipl.getNChannels(), ipl.getNSlices(), ipl.getNFrames());
        }

        // Adding the overlay element
            int nThreads = multithread ? Prefs.getThreads() : 1;
            ThreadPoolExecutor pool = new ThreadPoolExecutor(nThreads,nThreads,0L,TimeUnit.MILLISECONDS,new LinkedBlockingQueue<>());

            // Running through each object, adding it to the overlay along with an ID label
            AtomicInteger count = new AtomicInteger();
            for (Obj object:inputObjects.values()) {
                ImagePlus finalIpl = ipl;

                Runnable task = () -> {
                    float hue = hues.get(object.getID());
                    Color colour = getColour(hue,opacity);
                
                    addOverlay(object, finalIpl, colour, pointSize, pointType, lineWidth, posMeasurements, radiusMeasurement, renderInAllFrames);

                    writeMessage("Rendered " + (count.incrementAndGet()) + " objects of " + inputObjects.size());
                    
                };
                pool.submit(task);
            }

            pool.shutdown();
        try {
            pool.awaitTermination(Integer.MAX_VALUE, TimeUnit.DAYS); // i.e. never terminate early
        } catch (InterruptedException e) {
            return false;
        }

        Image outputImage = new Image(outputImageName,ipl);

        // If necessary, adding output image to workspace.  This also allows us to show it.
        if (addOutputToWorkspace) workspace.addImage(outputImage);
        if (showOutput) outputImage.showImage();

        return true;

    }

    @Override
    protected void initialiseParameters() {
        super.initialiseParameters();

        parameters.add(new ParamSeparatorP(INPUT_SEPARATOR,this));
        parameters.add(new InputImageP(INPUT_IMAGE, this));
        parameters.add(new InputObjectsP(INPUT_OBJECTS, this));

        parameters.add(new ParamSeparatorP(OUTPUT_SEPARATOR,this));
        parameters.add(new BooleanP(APPLY_TO_INPUT, this,false));
        parameters.add(new BooleanP(ADD_OUTPUT_TO_WORKSPACE, this,false));
        parameters.add(new OutputImageP(OUTPUT_IMAGE, this));

        parameters.add(new ParamSeparatorP(POSITION_SEPARATOR,this));
        parameters.add(new ObjectMeasurementP(X_POSITION_MEASUREMENT, this));
        parameters.add(new ObjectMeasurementP(Y_POSITION_MEASUREMENT, this));
        parameters.add(new ObjectMeasurementP(Z_POSITION_MEASUREMENT, this));
        parameters.add(new BooleanP(USE_RADIUS, this,true));
        parameters.add(new ObjectMeasurementP(MEASUREMENT_FOR_RADIUS, this));

        parameters.add(new ParamSeparatorP(RENDERING_SEPARATOR,this));
        parameters.add(new DoubleP(LINE_WIDTH,this,1));
        parameters.add(new ChoiceP(POINT_SIZE,this,PointSizes.SMALL,PointSizes.ALL));
        parameters.add(new ChoiceP(POINT_TYPE,this,PointTypes.CIRCLE,PointTypes.ALL));
        parameters.add(new DoubleP(OPACITY, this, 100));
        parameters.add(new BooleanP(RENDER_IN_ALL_FRAMES,this,false));

        parameters.add(new ParamSeparatorP(EXECUTION_SEPARATOR,this));
        parameters.add(new BooleanP(ENABLE_MULTITHREADING, this, true));

    }

    @Override
    public ParameterCollection updateAndGetParameters() {
        String inputObjectsName = parameters.getValue(INPUT_OBJECTS);

        ParameterCollection returnedParameters = new ParameterCollection();

        returnedParameters.add(parameters.getParameter(INPUT_SEPARATOR));
        returnedParameters.add(parameters.getParameter(INPUT_IMAGE));
        returnedParameters.add(parameters.getParameter(INPUT_OBJECTS));

        returnedParameters.add(parameters.getParameter(OUTPUT_SEPARATOR));
        returnedParameters.add(parameters.getParameter(APPLY_TO_INPUT));
        if (!(boolean) parameters.getValue(APPLY_TO_INPUT)) {
            returnedParameters.add(parameters.getParameter(ADD_OUTPUT_TO_WORKSPACE));

            if ((boolean) parameters.getValue(ADD_OUTPUT_TO_WORKSPACE)) {
                returnedParameters.add(parameters.getParameter(OUTPUT_IMAGE));

            }
        }

        returnedParameters.add(parameters.getParameter(POSITION_SEPARATOR));
        returnedParameters.add(parameters.getParameter(X_POSITION_MEASUREMENT));
        returnedParameters.add(parameters.getParameter(Y_POSITION_MEASUREMENT));
        returnedParameters.add(parameters.getParameter(Z_POSITION_MEASUREMENT));

        ((ObjectMeasurementP) parameters.getParameter(X_POSITION_MEASUREMENT)).setObjectName(inputObjectsName);
        ((ObjectMeasurementP) parameters.getParameter(Y_POSITION_MEASUREMENT)).setObjectName(inputObjectsName);
        ((ObjectMeasurementP) parameters.getParameter(Z_POSITION_MEASUREMENT)).setObjectName(inputObjectsName);

        returnedParameters.add(parameters.getParameter(USE_RADIUS));
        if ((boolean) parameters.getValue(USE_RADIUS)) {
            returnedParameters.add(parameters.getParameter(MEASUREMENT_FOR_RADIUS));
            ((ObjectMeasurementP) parameters.getParameter(MEASUREMENT_FOR_RADIUS)).setObjectName(inputObjectsName);
            returnedParameters.add(parameters.getParameter(RENDERING_SEPARATOR));
            returnedParameters.add(parameters.getParameter(LINE_WIDTH));
        } else {
            returnedParameters.add(parameters.getParameter(RENDERING_SEPARATOR));
            returnedParameters.add(parameters.getParameter(POINT_SIZE));
            returnedParameters.add(parameters.getParameter(POINT_TYPE));
        }

        returnedParameters.addAll(super.updateAndGetParameters(inputObjectsName));
        returnedParameters.add(parameters.getParameter(OPACITY));
        returnedParameters.add(parameters.getParameter(RENDER_IN_ALL_FRAMES));

        returnedParameters.add(parameters.getParameter(EXECUTION_SEPARATOR));
        returnedParameters.add(parameters.getParameter(ENABLE_MULTITHREADING));

        return returnedParameters;

    }

    @Override
    public ImageMeasurementRefCollection updateAndGetImageMeasurementRefs() {
        return null;
    }

    @Override
    public ObjMeasurementRefCollection updateAndGetObjectMeasurementRefs() {
        return null;
    }

    @Override
    public MetadataRefCollection updateAndGetMetadataReferences() {
        return null;
    }

    @Override
    public boolean verify() {
        return true;
    }
}
