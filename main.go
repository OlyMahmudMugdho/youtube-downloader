package main

import (
	"embed"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
)

//go:embed target/image/**
var embeddedFiles embed.FS

func main() {
	tmpDir, err := os.MkdirTemp("", "embedded-java")
	if err != nil {
		panic(err)
	}
	defer os.RemoveAll(tmpDir)

	// Extract only target/image
	err = fsWalkAndExtract("target/image", tmpDir)
	if err != nil {
		panic(err)
	}

	// Decide which java binary to run
	var javaBin string
	if runtime.GOOS == "windows" {
		javaBin = filepath.Join(tmpDir, "target", "image", "bin", "java.exe")
	} else {
		javaBin = filepath.Join(tmpDir, "target", "image", "bin", "java")
	}

	// Run module
	cmd := exec.Command(javaBin, "-m", "com.mahmud/com.mahmud.App")
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	fmt.Println("Running:", cmd.String())
	if err := cmd.Run(); err != nil {
		panic(err)
	}
}

func fsWalkAndExtract(prefix, dest string) error {
	return fsWalk(prefix, func(path string, d []byte) error {
		relPath := path[len(prefix):]
		destPath := filepath.Join(dest, prefix, relPath)

		if err := os.MkdirAll(filepath.Dir(destPath), 0755); err != nil {
			return err
		}
		return os.WriteFile(destPath, d, 0755)
	})
}

func fsWalk(root string, fn func(path string, data []byte) error) error {
	entries, err := embeddedFiles.ReadDir(root)
	if err != nil {
		return err
	}
	for _, e := range entries {
		fullPath := filepath.Join(root, e.Name())
		if e.IsDir() {
			if err := fsWalk(fullPath, fn); err != nil {
				return err
			}
		} else {
			data, err := embeddedFiles.ReadFile(fullPath)
			if err != nil {
				return err
			}
			if err := fn(fullPath, data); err != nil {
				return err
			}
		}
	}
	return nil
}
