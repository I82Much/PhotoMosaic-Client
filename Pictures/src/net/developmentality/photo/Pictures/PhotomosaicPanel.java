/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.developmentality.photo.Pictures;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.developmentality.photo.Metadata;
import net.developmentality.photo.PhotoMosaic;
import net.miginfocom.swing.MigLayout;
import org.openide.util.Exceptions;
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

    public PhotomosaicPanel(Map<File, Metadata> photoIndex) {
        super(new MigLayout("insets 0, fill"));
        this.photoIndex = photoIndex;
        
        photos = new JPanel(new MigLayout("fill"));
        photos.setBackground(Color.BLUE);

        saveImageButton = new JButton(new SaveImageAction());
        pickTargetImageButton = new JButton(new PickTargetAction());
        createPhotoMosaicButton = new JButton(new CreatePhotoMosaicAction());

        add(photos, "grow, wrap");
        add(createPhotoMosaicButton, "split 3");
        add(pickTargetImageButton);
        add(saveImageButton);

        createPhotoMosaicButton.setEnabled(false);
    }

    public void setTargetImage(BufferedImage targetImage) {
        this.targetImage = targetImage;
        createPhotoMosaicButton.setEnabled(true);
    }



    private class CreatePhotoMosaicAction extends AbstractAction {
        public CreatePhotoMosaicAction() {
            super("Create photomosaic");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(new CreatePhotoMosaic());
        }
    }

    private class CreatePhotoMosaic implements Runnable {
        @Override
        public void run() {
            int numRows = 30;
            int numCols = 40;

            Seq<BufferedImage>[][] imageGrid =
            PhotoMosaic.createMosaic(targetImage, photoIndex, 0.0f, 0, 0, numRows, numCols);

            System.out.println("Finished");
            System.out.println(Arrays.toString(imageGrid));
        }
    }

    private class SaveImageAction extends AbstractAction {
        public SaveImageAction() {
            super("Save image");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
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
