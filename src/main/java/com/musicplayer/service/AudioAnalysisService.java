package com.musicplayer.service;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.ComplexOnsetDetector;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Service untuk analisis audio menggunakan TarsosDSP
 * Ekstraksi BPM, pitch, dan fitur audio lainnya
 */
public class AudioAnalysisService {

    /**
     * Deteksi BPM (tempo) dari file audio
     */
    public int detectBPM(File audioFile) {
        try {
            AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, 2048, 0);

            final List<Double> onsetTimes = new ArrayList<>();

            OnsetHandler onsetHandler = (time, salience) -> onsetTimes.add(time);

            ComplexOnsetDetector onsetDetector = new ComplexOnsetDetector(2048);
            onsetDetector.setHandler(onsetHandler);

            dispatcher.addAudioProcessor(onsetDetector);
            dispatcher.run();

            // Calculate BPM from onset times
            if (onsetTimes.size() < 4) {
                return 120; // Default BPM
            }

            // Calculate average time between onsets
            double totalInterval = 0;
            for (int i = 1; i < onsetTimes.size(); i++) {
                totalInterval += onsetTimes.get(i) - onsetTimes.get(i - 1);
            }
            double avgInterval = totalInterval / (onsetTimes.size() - 1);

            // Convert to BPM
            int bpm = (int) (60.0 / avgInterval);

            // Normalize to typical music range (60-180 BPM)
            while (bpm < 60) bpm *= 2;
            while (bpm > 180) bpm /= 2;

            return bpm;

        } catch (Exception e) {
            e.printStackTrace();
            return 120; // Default fallback
        }
    }

    /**
     * Deteksi pitch dominan dari file audio
     */
    public double detectPitch(File audioFile) {
        try {
            AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, 2048, 0);

            final List<Float> pitches = new ArrayList<>();

            PitchDetectionHandler pdh = (result, audioEvent) -> {
                if (result.getPitch() != -1) {
                    pitches.add(result.getPitch());
                }
            };

            PitchProcessor pitchProcessor = new PitchProcessor(
                    PitchProcessor.PitchEstimationAlgorithm.YIN,
                    44100,
                    2048,
                    pdh
            );

            dispatcher.addAudioProcessor(pitchProcessor);
            dispatcher.run();

            // Calculate median pitch
            if (pitches.isEmpty()) {
                return 0;
            }

            pitches.sort(Float::compareTo);
            return pitches.get(pitches.size() / 2);

        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Analisis mood berdasarkan karakteristik audio
     * Simplified mood detection based on tempo and pitch
     */
    public String analyzeMood(int bpm, double pitch) {
        if (bpm > 140) {
            return pitch > 300 ? "Energetic" : "Intense";
        } else if (bpm > 100) {
            return pitch > 300 ? "Happy" : "Upbeat";
        } else if (bpm > 80) {
            return pitch > 300 ? "Calm" : "Relaxed";
        } else {
            return pitch > 300 ? "Melancholic" : "Sad";
        }
    }
}