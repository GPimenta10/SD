/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Utils;

import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.*;
import java.awt.*;

/**
 * 
 * 
 */
public class MinimalScrollBarUI extends BasicScrollBarUI {

    private static final int LARGURA_BARRA = 6;

    private Color corThumb;
    private Color corThumbHover;
    private Color corTrack;
    
    /**
     * 
     * 
     * @param c
     * @return 
     */
    @Override
    public Dimension getPreferredSize(JComponent c) {
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            return new Dimension(LARGURA_BARRA, super.getPreferredSize(c).height);
        } else {
            return new Dimension(super.getPreferredSize(c).width, LARGURA_BARRA);
        }
    }
    
    /**
     * 
     * 
     */
    @Override
    protected void configureScrollBarColors() {

        // Cores modernas do FlatLaf
        corThumb = UIManager.getColor("ScrollBar.thumb");
        corThumbHover = UIManager.getColor("ScrollBar.thumbHover");
        corTrack = UIManager.getColor("ScrollBar.track");

        if (corThumb == null) corThumb = new Color(120, 120, 120);
        if (corThumbHover == null) corThumbHover = new Color(160, 160, 160);
        if (corTrack == null) corTrack = UIManager.getColor("Panel.background");

        this.thumbColor = corThumb;
        this.trackColor = corTrack;
    }
    
    /**
     * 
     * @param orientation
     * @return 
     */
    @Override
    protected JButton createDecreaseButton(int orientation) {
        return criarBotaoVazio();
    }
    
    /**
     * 
     * 
     * @param orientation
     * @return 
     */
    @Override
    protected JButton createIncreaseButton(int orientation) {
        return criarBotaoVazio();
    }
        
    /**
     * 
     * 
     * @param g
     * @param c
     * @param r 
     */
    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle r) {

        if (!scrollbar.isEnabled()) return;

        Graphics2D g2 = (Graphics2D) g.create();

        // Hover moderno
        Color cor = isThumbRollover() ? corThumbHover : corThumb;

        g2.setColor(cor);
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 15, 15);
        g2.dispose();
    }
    
    /**
     * 
     * 
     * @param g
     * @param c
     * @param r 
     */
    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(corTrack);
        g2.fillRect(r.x, r.y, r.width, r.height);
        g2.dispose();
    }
        
    /**
     * 
     * @return 
     */
    private JButton criarBotaoVazio() {
        JButton botao = new JButton();
        botao.setPreferredSize(new Dimension(0, 0));
        botao.setOpaque(false);
        botao.setContentAreaFilled(false);
        botao.setBorderPainted(false);
        return botao;
    }
}
