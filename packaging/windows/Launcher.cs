using System;
using System.Diagnostics;
using System.IO;
using System.IO.Compression;
using System.Reflection;
using System.Security.Cryptography;
using System.Threading;
using System.Text;
using System.Runtime.InteropServices;
using System.Windows.Forms;

// A single distributable EXE. The embedded game and Java runtime are extracted
// once per build into the user's application cache, never beside the EXE.
internal static class Launcher
{
    [STAThread]
    private static void Main(string[] args)
    {
        string logPath = null;
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

                string cache = SelectCacheRoot(new string[] {
                    Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "TankOps", "versions"),
                    Path.Combine(Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location), ".tankops-cache", "versions")
                });
                string gameDirectory = Path.Combine(cache, version);
                string executable = Path.Combine(gameDirectory, "runtime", "bin", "java.exe");

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

                string jar = Path.Combine(gameDirectory, "app", "TankOps.jar");
                string jvm = Path.Combine(gameDirectory, "runtime", "bin", "server", "jvm.dll");
                if (!File.Exists(executable) || !File.Exists(jar) || !File.Exists(jvm))
                    throw new IOException("Du lieu Java/game trong bo nho dem bi thieu. Kiem tra Protection history cua Windows Security. Thu muc: " + gameDirectory);

                string logs = Path.Combine(cache, "..", "logs");
                Directory.CreateDirectory(logs);
                logPath = Path.GetFullPath(Path.Combine(logs, DateTime.Now.ToString("yyyyMMdd-HHmmss-fff") + "-" + Process.GetCurrentProcess().Id + ".log"));
                bool diagnose = Array.IndexOf(args, "--diagnose") >= 0;
                using (TextWriter log = TextWriter.Synchronized(new StreamWriter(logPath, false, Encoding.UTF8) { AutoFlush = true }))
                using (Process process = new Process())
                {
                    log.WriteLine("TankOps launcher: " + DateTime.Now.ToString("O"));
                    log.WriteLine("OS: " + Environment.OSVersion + "; 64-bit OS: " + Environment.Is64BitOperatingSystem);
                    log.WriteLine("Runtime: " + executable);
                    process.StartInfo = new ProcessStartInfo
                    {
                        FileName = executable,
                        Arguments = diagnose ? "-version" : "-Dfile.encoding=UTF-8 -jar app\\TankOps.jar",
                        WorkingDirectory = gameDirectory,
                        UseShellExecute = false,
                        CreateNoWindow = true,
                        RedirectStandardOutput = true,
                        RedirectStandardError = true
                    };
                    // The bundled runtime must not inherit JVM options from an unrelated JDK.
                    foreach (string key in new string[] { "JAVA_TOOL_OPTIONS", "_JAVA_OPTIONS", "JDK_JAVA_OPTIONS" })
                        process.StartInfo.EnvironmentVariables.Remove(key);
                    process.OutputDataReceived += delegate(object sender, DataReceivedEventArgs e) { if (e.Data != null) log.WriteLine(e.Data); };
                    process.ErrorDataReceived += delegate(object sender, DataReceivedEventArgs e) { if (e.Data != null) log.WriteLine(e.Data); };
                    process.Start();
                    process.BeginOutputReadLine();
                    process.BeginErrorReadLine();
                    process.WaitForExit();
                    log.WriteLine("Java exit code: " + process.ExitCode);
                    if (process.ExitCode != 0)
                        throw new IOException("Java khong khoi dong/chay thanh cong (exit code " + process.ExitCode + ").");
                }
            }
        }
        catch (Exception error)
        {
            if (logPath != null)
            {
                try { File.AppendAllText(logPath, Environment.NewLine + error.ToString()); } catch { }
            }
            MessageBox.Show("Khong the mo TankOps.\n\n" + error.Message
                + (logPath != null ? "\n\nGui file log nay de kiem tra:\n" + logPath : ""),
                "TankOps", MessageBoxButtons.OK, MessageBoxIcon.Error);
            Environment.ExitCode = 1;
        }
    }

    [DllImport("kernel32.dll", CharSet = CharSet.Unicode, SetLastError = true)]
    private static extern uint GetShortPathName(string longPath, StringBuilder shortPath, uint bufferLength);

    private static bool IsAscii(string path)
    {
        foreach (char c in path) if (c > 127) return false;
        return true;
    }

    private static string SelectCacheRoot(string[] candidates)
    {
        foreach (string candidate in candidates)
        {
            try
            {
                Directory.CreateDirectory(candidate);
                string path = Path.GetFullPath(candidate);
                if (!IsAscii(path))
                {
                    StringBuilder shortPath = new StringBuilder(32768);
                    uint length = GetShortPathName(path, shortPath, (uint)shortPath.Capacity);
                    if (length == 0 || length >= shortPath.Capacity || !IsAscii(shortPath.ToString())) continue;
                    path = shortPath.ToString();
                }
                string probe = Path.Combine(path, Guid.NewGuid().ToString("N") + ".probe");
                using (FileStream stream = new FileStream(probe, FileMode.CreateNew, FileAccess.Write,
                    FileShare.None, 1, FileOptions.DeleteOnClose)) { }
                return path;
            }
            catch (IOException) { }
            catch (UnauthorizedAccessException) { }
        }
        throw new IOException("Java khong ho tro duong dan hien tai. Hay chuyen TankOps.exe vao thu muc co quyen ghi, chi dung ky tu khong dau (vi du D:\\Games\\TankOps), roi mo lai.");
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
