import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rtHelpers.HitRecord;
import rtHelpers.Ray3D;
import util.Light;
import util.Material;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;


/**
 * Created by ashesh on 9/18/2015.
 *
 * The View class is the "controller" of all our OpenGL stuff. It cleanly
 * encapsulates all our OpenGL functionality from the rest of Java GUI, managed
 * by the JOGLFrame class.
 */
public class View {
  private int WINDOW_WIDTH, WINDOW_HEIGHT;
  private Stack<Matrix4f> modelView;
  private Matrix4f projection, trackballTransform;
  private float trackballRadius;
  private Vector2f mousePos;


  private util.ShaderProgram program;
  private util.ShaderLocationsVault shaderLocations;
  private int projectionLocation;
  private sgraph.IScenegraph<VertexAttrib> scenegraph;

  // Raytracer
  private boolean isRaytrace;
  private List<Light> lights;


  public View() {
    projection = new Matrix4f();
    modelView = new Stack<Matrix4f>();
    trackballRadius = 300;
    trackballTransform = new Matrix4f();
    scenegraph = null;
    isRaytrace = false;
  }

  public void initScenegraph(GLAutoDrawable gla, InputStream in) throws Exception {
    GL3 gl = gla.getGL().getGL3();

    if (scenegraph != null)
      scenegraph.dispose();

    program.enable(gl);

    scenegraph = sgraph.SceneXMLReader.importScenegraph(in, new VertexAttribProducer());

    sgraph.IScenegraphRenderer renderer = new sgraph.GL3ScenegraphRenderer();
    renderer.setContext(gla);
    Map<String, String> shaderVarsToVertexAttribs = new HashMap<String, String>();
    shaderVarsToVertexAttribs.put("vPosition", "position");
    shaderVarsToVertexAttribs.put("vNormal", "normal");
    shaderVarsToVertexAttribs.put("vTexCoord", "texcoord");
    renderer.initShaderProgram(program, shaderVarsToVertexAttribs);
    scenegraph.setRenderer(renderer);
    program.disable(gl);
  }

  public void init(GLAutoDrawable gla) throws Exception {
    GL3 gl = gla.getGL().getGL3();


    //compile and make our shader program. Look at the ShaderProgram class for details on how this is done
    program = new util.ShaderProgram();

    program.createProgram(gl, "shaders/phong-multiple.vert",
            "shaders/phong-multiple.frag");

    shaderLocations = program.getAllShaderVariables(gl);

    //get input variables that need to be given to the shader program
    projectionLocation = shaderLocations.getLocation("projection");
  }


  public void draw(GLAutoDrawable gla) {
    GL3 gl = gla.getGL().getGL3();

    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
    gl.glEnable(gl.GL_DEPTH_TEST);


    program.enable(gl);

    while (!modelView.empty())
      modelView.pop();

        /*
         *In order to change the shape of this triangle, we can either move the vertex positions above, or "transform" them
         * We use a modelview matrix to store the transformations to be applied to our triangle.
         * Right now this matrix is identity, which means "no transformations"
         */
    modelView.push(new Matrix4f());
    modelView.peek().lookAt(new Vector3f(150,150,150), new Vector3f(0, 0,
            0),
            new Vector3f(0, 1, 0))
            .mul(trackballTransform);

    lights = scenegraph.getLights(modelView);

    if (isRaytrace) {
      raytrace(WINDOW_WIDTH, WINDOW_HEIGHT, modelView);
      isRaytrace = false;
    } else {
      drawNormal(gla);
    }

  }

  public void mousePressed(int x, int y) {
    mousePos = new Vector2f(x, y);
  }

  public void mouseReleased(int x, int y) {
    System.out.println("Released");
  }

  public void mouseDragged(int x, int y) {
    Vector2f newM = new Vector2f(x, y);

    Vector2f delta = new Vector2f(newM.x - mousePos.x, newM.y - mousePos.y);
    mousePos = new Vector2f(newM);

    trackballTransform = new Matrix4f().rotate(delta.x / trackballRadius, 0, 1, 0)
            .rotate(delta.y / trackballRadius, 1, 0, 0)
            .mul(trackballTransform);
  }

  public void reshape(GLAutoDrawable gla, int x, int y, int width, int height) {
    GL gl = gla.getGL();
    WINDOW_WIDTH = width;
    WINDOW_HEIGHT = height;
    gl.glViewport(0, 0, width, height);

    projection = new Matrix4f().perspective((float) Math.toRadians(120.0f),
            (float) width / height, 0.1f, 10000.0f);
    // proj = new Matrix4f().ortho(-400,400,-400,400,0.1f,10000.0f);

  }

  public void dispose(GLAutoDrawable gla) {
    GL3 gl = gla.getGL().getGL3();

  }

  public void setRaytrace(boolean isRaytrace) {
    this.isRaytrace = isRaytrace;
  }

  // Normal openGL drawing that we do
  private void drawNormal(GLAutoDrawable gla) {
    GL3 gl = gla.getGL().getGL3();
    FloatBuffer fb16 = Buffers.newDirectFloatBuffer(16);

    gl.glClearColor(0, 0, 0, 1);
    gl.glClear(gl.GL_COLOR_BUFFER_BIT | gl.GL_DEPTH_BUFFER_BIT);
    gl.glEnable(GL.GL_DEPTH_TEST);

    program.enable(gl);

    gl.glUniformMatrix4fv(shaderLocations.getLocation("projection"),
            1,
            false, projection.get(fb16));
    gl.glUniformMatrix4fv(shaderLocations.getLocation("texturematrix"),
            1, false, new Matrix4f().identity().get(fb16));

    scenegraph.draw(modelView);

    /*
     *OpenGL batch-processes all its OpenGL commands.
     *  *The next command asks OpenGL to "empty" its batch of issued commands, i.e. draw
     *
     *This a non-blocking function. That is, it will signal OpenGL to draw, but won't wait for it to
     *finish drawing.
     *
     *If you would like OpenGL to start drawing and wait until it is done, call glFinish() instead.
     */
    gl.glFlush();

    program.disable(gl);
  }

  private void raytrace(int w, int h, Stack<Matrix4f> mv) {
    BufferedImage outputImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

    Vector4f startPoint = new Vector4f(0, 0, 0, 1);

    for (int i = 0; i < w; i++) {
      for (int j = 0; j < h; j++) {
        Ray3D ray3D = new Ray3D(startPoint, i, j, w, h, (float) Math.toRadians(120));


        List<HitRecord> hitRecords = scenegraph.raycast(ray3D, mv);

        // Color
        Vector4f color = new Vector4f(1, 1, 1, 0);

        if (hitRecords.size() > 0) {
          color = shade(hitRecords.get(0));
        }

        Color c = new Color(color.x, color.y, color.z);

        outputImage.setRGB(i, h - j - 1, c.getRGB());
      }
    }

    System.out.println("DONE");

    OutputStream out = null;

    try {
      out = new FileOutputStream("output/raytrace.png");
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException("Unable to output image :(");
    }

    try {
      ImageIO.write(outputImage, "png", out);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to output image :(");
    }
  }

  private Vector4f shade(HitRecord hitRecord) {
    Vector4f color = new Vector4f(0, 0, 0, 0);
    Vector3f normalDir, lightDir, viewDir, reflectDir;
    Vector3f ambient, diffuse, specular;
    float nDotL, rDotV;

    Matrix4f transformation = new Matrix4f(hitRecord.transform);
    Matrix4f invTrans = new Matrix4f(transformation).invert().transpose();
    Vector4f fPosn = hitRecord.get(hitRecord.intersectionIn);

    transformation.transform(fPosn);

    Vector4f fNormal = hitRecord.get(hitRecord.normalIn);

    invTrans.transform(fNormal);

    Material material = new Material(hitRecord.material);

    Vector3f materialAmbient = new Vector3f(material.getAmbient().x, material.getAmbient().y, material.getAmbient().z);
    Vector3f materialDiffuse = new Vector3f(material.getDiffuse().x, material.getDiffuse().y, material.getDiffuse().z);
    Vector3f materialSpecular = new Vector3f(material.getSpecular().x, material.getSpecular().y, material.getSpecular().z);

    float materialAbsorpotion = material.getAbsorption();
    float materialReflection = material.getReflection();
    float materialRefraction = material.getRefractiveIndex();
    float materialShine = material.getShininess();
    float materialTrans = material.getTransparency();

    // Lighting information
    Vector4f lightPos, lightSpotDir;
    Vector3f lightAmbient, lightDiffuse, lightSpec;
    float lightSpotAngle;

    Vector4f fColor = new Vector4f(0, 0, 0, 0);

    int numLights = lights.size();

    for (int i = 0; i < numLights; i++) {
      Light l = lights.get(i);
      lightPos = l.getPosition();
      lightSpotDir = l.getSpotDirection();
      lightAmbient = l.getAmbient();
      lightDiffuse = l.getDiffuse();
      lightSpec = l.getSpecular();
      lightSpotAngle = l.getSpotCutoff();

      if (lightPos.w != 0) {
        lightDir = new Vector3f(lightPos.x - fPosn.x, lightPos.y - fPosn.y, lightPos.z - fPosn.z).normalize();
      } else {
        lightDir = new Vector3f(-lightPos.x, -lightPos.y, -lightPos.z).normalize();
      }

      Vector3f spotDir = new Vector3f(lightSpotDir.x, lightSpotDir.y, lightSpotDir.z).normalize();
      if (-lightDir.dot(spotDir) < (float) (Math.cos(Math.toRadians(lightSpotAngle)))) {
        continue;
      }

      Vector3f tNormal = new Vector3f(fNormal.x, fNormal.y, fNormal.z);
      normalDir = new Vector3f(tNormal).normalize();

      nDotL = normalDir.dot(lightDir);

      viewDir = new Vector3f(-fPosn.x, -fPosn.y, -fPosn.z).normalize();
      reflectDir = new Vector3f(lightDir).negate().reflect(normalDir).normalize();
      rDotV = (float) (Math.max(reflectDir.dot(viewDir), 0));

      ambient = lightAmbient.mul(materialAmbient);
      diffuse = lightDiffuse.mul(materialDiffuse).mul((float) Math.max(nDotL, 0));

      if (nDotL > 0) {
        specular = lightSpec.mul(materialSpecular).mul((float) (Math.pow(rDotV, materialShine)));
      } else {
        specular = new Vector3f(0, 0, 0);
      }

      fColor = fColor.add(new Vector4f(new Vector3f(ambient.add(diffuse).add(specular)), 1)).normalize();
    }

    return fColor;
  }


}
