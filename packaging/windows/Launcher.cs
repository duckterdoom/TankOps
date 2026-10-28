using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Security.Cryptography;
using System.Threading;
using System.Windows.Forms;

// A single distributable EXE. The embedded game and Java runtime are extracted
// once per build into the user's application cache, never beside the EXE.
internal static class Launcher
{
    [STAThread]
    private static void Main()
    {
        try
        {
            using (Stream payload = Assembly.GetExecutingAssembly().GetManifestResourceStream("TankOps.Payload.zip"))
            {
                if (payload == null)
                    throw new InvalidOperationException("Khong tim thay du lieu game trong file exe.");

                string version;
                using (SHA256 sha = SHA256.Create())
                    version = BitConverter.ToString(sha.ComputeHash(payload)).Replace("-", "").ToLowerInvariant();
                payload.Position = 0;

                string cache = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "TankOps", "versions");
                Directory.CreateDirectory(cache);
                string gameDirectory = Path.Combine(cache, version);
                string executable = Path.Combine(gameDirectory, "TankOps.exe");

                // Serialize first launches of the same build. A completed cache
                // is published only after every file has been extracted.
                using (FileStream buildLock = AcquireLock(Path.Combine(cache, version + ".lock")))
                {
                    if (!Directory.Exists(gameDirectory))
                    {
                        string staging = Path.Combine(cache, version + ".tmp-" + Guid.NewGuid().ToString("N"));
                        Directory.CreateDirectory(staging);
                        try
                        {
                            using (ZipArchive archive = new ZipArchive(payload, ZipArchiveMode.Read, true))
                                archive.ExtractToDirectory(staging);
                            Directory.Move(staging, gameDirectory);
                        }
                        finally
                        {
                            if (Directory.Exists(staging))
                                Directory.Delete(staging, true);
                        }
                    }
                }

                if (!File.Exists(executable) || !File.Exists(Path.Combine(gameDirectory, "app", "TankOps.jar")))
                    throw new IOException("Du lieu game trong bo nho dem bi thieu. Hay xoa thu muc: " + gameDirectory);

                Process.Start(new ProcessStartInfo
                {
                    FileName = executable,
                    WorkingDirectory = gameDirectory,
                    UseShellExecute = false
                });
            }
        }
        catch (Exception error)
        {
            MessageBox.Show("Khong the mo TankOps.\n\n" + error.Message, "TankOps", MessageBoxButtons.OK, MessageBoxIcon.Error);
            Environment.ExitCode = 1;
        }
    }

    private static FileStream AcquireLock(string path)
    {
        Stopwatch timer = Stopwatch.StartNew();
        while (true)
        {
            try
            {
                return new FileStream(path, FileMode.OpenOrCreate, FileAccess.ReadWrite, FileShare.None);
            }
            catch (IOException)
            {
                if (timer.Elapsed.TotalSeconds >= 60)
                    throw new IOException("Game dang chuan bi du lieu. Hay doi mot chut roi mo lai.");
                Thread.Sleep(250);
            }
        }
    }
}
