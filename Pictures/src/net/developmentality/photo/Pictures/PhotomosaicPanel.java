/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.developmentality.photo.Pictures;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import net.developmentality.photo.Metadata;
import net.developmentality.photo.PhotoMosaic;
import net.developmentality.photo.PhotoMosaicCallback;
import net.miginfocom.swing.MigLayout;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Cancellable;
import org.openide.util.Exceptions;
import org.openide.util.RequestProcessor;
import scala.collection.Seq;
import scala.collection.immutable.Map;

/**
 *
 * @author nicholasdunn
 */
public class PhotomosaicPanel extends JPanel {

    private JPanel photos;
    private JButton saveImageButton;
    private JButton pickTargetImageButton;
    private JButton createPhotoMosaicButton;
    private Map<File, Metadata> photoIndex;
    private BufferedImage targetImage;

    static int counter = 0;

    private static final RequestProcessor THREAD_POOL = new RequestProcessor("PhotoMosaic threadpool", 1);

    private BufferedImage photoMosaic;

    public PhotomosaicPanel(Map<File, Metadata> photoIndex) {
        super(new MigLayout("insets 0, fill, debug"));
        this.photoIndex = photoIndex;
        
        photos = new JPanel() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (photoMosaic == null) {
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
                else {
                    g.drawImage(photoMosaic, 0, 0, getWidth(), getHeight(), null);
                }
            }
        };

        saveImageButton = new JButton(new SaveImageAction());
        pickTargetImageButton = new JButton(new PickTargetAction());
        createPhotoMosaicButton = new JButton(new CreatePhotoMosaicAction());

        add(photos, "span 3, grow 400, wrap");
        add(createPhotoMosaicButton);
        add(pickTargetImageButton);
        add(saveImageButton);

        createPhotoMosaicButton.setEnabled(false);
        try {
//            setTargetImage(ImageIO.read(new File("/Users/ndunn/Desktop/Screen shot 2010-12-06 at 10.31.52 PM.png")));
            setTargetImage(ImageIO.read(new File("/Users/ndunn/Dropbox/Photos/Old Rag/IMG_4286.jpg")));
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
    }

//    public void setImage(int row, int column, BufferedImage img) {
//        photos.add(new JLabel(new ImageIcon(img)));
//    }

    private void setPhotomosaic(BufferedImage completedImage) {
        this.photoMosaic = completedImage;
//        photos.add(new JLabel(new ImageIcon(completedImage)));
        repaint();
    }

    public void setTargetImage(BufferedImage targetImage) {
        System.out.println("setting target image to " + targetImage);
        this.targetImage = targetImage;
        createPhotoMosaicButton.setEnabled(true);
    }

    private class CreatePhotoMosaicAction extends AbstractAction {
        public CreatePhotoMosaicAction() {
            super("Create photomosaic");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Runnable createPhotomosaic = new CreatePhotoMosaic();
            RequestProcessor.Task task = THREAD_POOL.create(createPhotomosaic);
            task.schedule(0);
        }
    }

    private class CreatePhotoMosaic implements Runnable {
        final ProgressHandle ph = ProgressHandleFactory.createHandle("Creating photomosaic", new Cancellable() {
            @Override
            public boolean cancel() {
//                return handleCancel();
                return true;
            }
        });
        int photosCompleted = 0;

        
        @Override
        public void run() {
            int numRows = 6;//15;
            int numCols = 8;//20;
            int totalPhotos = numRows * numCols;
            PhotoMosaicCallback callback = new PhotoMosaicCallback() {
                @Override
                public void photosCalculated(int row, int column, Seq<BufferedImage> seq) {
                    ph.progress(++photosCompleted);
//                    System.out.println("calculated photos for " + row + " column" + column);
//                    System.out.println(ph);
//                    PhotomosaicPanel.this.setImage(row, column, seq.first());
                }
            };
            ph.start(); //we must start the PH before we swith to determinate
            ph.switchToDeterminate(totalPhotos);
            Seq<BufferedImage>[][] imageGrid =
                PhotoMosaic.createMosaic(targetImage, photoIndex, 0.0f, 0, 0, numRows, numCols, callback);
            ph.finish();



            System.out.println("Finished");
//            System.out.println(Arrays.toString(imageGrid));

            int desiredWidth = 1024;
            int desiredHeight = 768;
            int thumbnailWidth = desiredWidth / numCols;
            int thumbnailHeight = desiredHeight / numCols;
            desiredWidth = thumbnailWidth * numCols;
            desiredHeight = thumbnailHeight * numRows;

            BufferedImage mosaic = new BufferedImage(desiredWidth, desiredHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = mosaic.createGraphics();
            for (int row = 0; row < numRows; row++) {
                for (int col = 0; col < numCols; col++) {
                    BufferedImage thumbnail = imageGrid[row][col].first();
                    int x = col * thumbnailWidth;
                    int y = row * thumbnailHeight;
                    g2.drawImage(thumbnail, x, y, thumbnailWidth, thumbnailHeight, null);
                }
            }
            setPhotomosaic(mosaic);
            
        }
    }

    private class SaveImageAction extends AbstractAction {
        public SaveImageAction() {
            super("Save image");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                File outputFile = new File("Photomosaic " + counter++ + ".png");
                ImageIO.write(photoMosaic, "png", outputFile);
                StatusDisplayer.getDefault().setStatusText("Successfully saved file to " + outputFile);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
    private class PickTargetAction extends AbstractAction {
        public PickTargetAction() {
            super("Pick target image");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JFileChooser chooser = new JFileChooser();
            int acceptValue = chooser.showOpenDialog(PhotomosaicPanel.this);
            if (acceptValue == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                try {
                    BufferedImage image = ImageIO.read(selected);
                    setTargetImage(image);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }


}
