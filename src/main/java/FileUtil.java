/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.IOException;

public class FileUtil
{
    private FileUtil() {} // Prevent instantiation

    public static boolean canReadWrite(File file)
    {
        return file.canRead() && file.canWrite();
    }

    public static boolean canReadWrite(String file)
    {
        return canReadWrite(new File(file));
    }

    public static boolean createFileWithDirs(File file)
    {
        if (exists(file))
            return true;

        else
        {
            file.getParentFile().mkdirs();
            if (!createNewFile(file))
                return false;
        }

        return true;
    }

    public static boolean createFileWithDirs(String file)
    {
        return createFileWithDirs(new File(file));
    }

    public static boolean createNewFile(File file)
    {
        try
        {
            if (!file.createNewFile())
                return false;
        } catch (IOException e)
        {
            return false;
        }

        return true;
    }

    public static boolean createNewFile(String file)
    {
        return createNewFile(new File(file));
    }

    public static boolean exists(File file)
    {
        return file.exists();
    }

    public static boolean exists(String file)
    {
        return exists(new File(file));
    }

    public static boolean hasData(File file)
    {
        return file.length() >= 1;
    }

    public static boolean hasData(String file)
    {
        return hasData(new File(file));
    }
}
