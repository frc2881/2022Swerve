// Copyright (c) 2022 FRC Team 2881 - The Lady Cans
//
// Open Source Software; you can modify and/or share it under the terms of BSD
// license file in the root directory of this project.

package frc.robot.subsystems;

import static frc.robot.Constants.kEnableDetailedLogging;

import java.util.Arrays;

import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.util.datalog.DataLog;
import edu.wpi.first.util.datalog.DoubleLogEntry;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class VisionTracking extends SubsystemBase {
  private final PhotonCamera m_visionCamera = new PhotonCamera("photonvision");
  private final PhotonCamera m_frontCamera = new PhotonCamera("frontCamera");
  private final DoubleLogEntry m_logArea;
  private final DoubleLogEntry m_logPitch;
  private final DoubleLogEntry m_logSkew;
  private final DoubleLogEntry m_logYaw;
  private final double[] m_yawHistory = new double[7];
  private final double[] m_yawCopy = new double[7];
  private final double[] m_pitchHistory = new double[7];
  private final double[] m_pitchCopy = new double[7];
  private double m_savedPitch;
  private double m_savedTime;
  private boolean m_disableVis = false;

  /** Creates a new VisionTracking. */
  public VisionTracking() {
    //NetworkTableInstance inst = NetworkTableInstance.getDefault();
    //NetworkTable table = inst.getTable("test");
    //NetworkTableEntry entry = table.getEntry("test");
    //entry.getLastChange();

    if(kEnableDetailedLogging) {
      DataLog log = DataLogManager.getLog();
      m_logArea = new DoubleLogEntry(log, "vision/area");
      m_logPitch = new DoubleLogEntry(log, "vision/pitch");
      m_logSkew = new DoubleLogEntry(log, "vision/skew");
      m_logYaw = new DoubleLogEntry(log, "vision/yaw");
    } else {
      m_logArea = null;
      m_logPitch = null;
      m_logSkew = null;
      m_logYaw = null;
    }
  }

  @Override
  public void periodic() {
    PhotonPipelineResult result;
    PhotonTrackedTarget target;
    double pitch;
    double yaw;

    result = m_visionCamera.getLatestResult();
    if(result.hasTargets()) {
      target = result.getBestTarget();
      pitch = target.getPitch();
      yaw = target.getYaw();

      if(kEnableDetailedLogging) {
        m_logArea.append(target.getArea());
        m_logPitch.append(pitch);
        m_logSkew.append(target.getSkew());
        m_logYaw.append(yaw);
      }
    } else {
      pitch = 1000;
      yaw = 0;
    }

    SmartDashboard.putNumber("Photon Vision Pitch", pitch);
    SmartDashboard.putNumber("Photon Vision Yaw", yaw);

    System.arraycopy(m_pitchHistory, 1, m_pitchHistory, 0, 6);
    System.arraycopy(m_yawHistory, 1, m_yawHistory, 0, 6);

    m_pitchHistory[6] = pitch;
    m_yawHistory[6] = yaw;
  }

  public void reset(){
    m_visionCamera.setDriverMode(false);
    m_frontCamera.setDriverMode(true);

    for(int i = 0; i < 7; i++) {
      m_yawHistory[i] = 0.0;
      m_pitchHistory[i] = 1000.0;
    }
  }

  public double computePitch(){
    for(int i = 0; i < 7; i++) {
      m_pitchCopy[i] = m_pitchHistory[i];
    }
    Arrays.sort(m_pitchCopy);

    double q1 = m_pitchCopy[1];
    double q3 = m_pitchCopy[5];

    double iqr = q3 - q1;

    double sum = 0;
    for(int i = 1; i < 6; i++) {
      sum += m_pitchCopy[i];
    }
    int count = 5;

    if(m_pitchCopy[6] < (q3 + (iqr * 1.5))) {
      sum += m_pitchCopy[6];
      count++;
    }

    if(m_pitchCopy[0] > (q1 - (iqr * 1.5))) {
      sum += m_pitchCopy[0];
      count++;
    }

    return sum / count;
  }

  public double computeYaw(){
    for(int i = 0; i < 7; i++) {
      m_yawCopy[i] = m_yawHistory[i];
    }
    Arrays.sort(m_yawCopy);

    double q1 = m_yawCopy[1];
    double q3 = m_yawCopy[5];

    double iqr = q3 - q1;

    double sum = 0;
    for(int i = 1; i < 6; i++) {
      sum += m_yawCopy[i];
    }
    int count = 5;

    if(m_yawCopy[6] < (q3 + (iqr * 1.5))) {
      sum += m_yawCopy[6];
      count++;
    }

    if(m_yawCopy[0] > (q1 - (iqr * 1.5))) {
      sum += m_yawCopy[0];
      count++;
    }

    return sum / count;
  }

  public double getYaw() {
    double yaw = computeYaw();

    if((SmartDashboard.getBoolean("Disable Vision", false) == true) ||
       (m_disableVis == true)) {
      // possibly want different outcome
      yaw = 0;
    } else if(yaw >= 1000) {
      yaw = 0;
    }

    return -yaw;
  }

  public void disableVFromController() {
    m_disableVis = true;
  }

  public void enableVFromController() {
    m_disableVis = false;
  }
}
